package com.blog.service;

import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.dto.QqInfoVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static com.blog.common.RequestUserAgent.clientIp;

@Service
public class QqInfoService {
    private static final Logger log = LoggerFactory.getLogger(QqInfoService.class);
    private static final Pattern QQ_PATTERN = Pattern.compile("^[1-9]\\d{4,10}$");
    private static final int DAILY_LIMIT = 100;
    private static final long IP_INTERVAL_MS = 15_000;
    private final ConcurrentHashMap<String, Long> lastQueryByIp = new ConcurrentHashMap<>();

    @Resource
    private RestTemplate restTemplate;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private DailyQuotaService dailyQuotaService;
    @Resource
    private LogService logService;

    @Value("${blog.uapi.qq-info-key:}")
    private String qqInfoKey;
    @Value("${blog.uapi.qq-userinfo-url:}")
    private String qqUserinfoUrl;
    @Value("${blog.uapi.qq-avatar-url:}")
    private String qqAvatarUrl;

    public QqInfoVO lookup(String raw, HttpServletRequest request) {
        String qq = raw == null ? "" : raw.trim();
        if (!QQ_PATTERN.matcher(qq).matches()) {
            throw new BizException(ErrorCode.PARAM_INVALID);
        }
        assertIpInterval(clientIp(request));
        if (!dailyQuotaService.tryAcquireQq(DAILY_LIMIT)) {
            throw new BizException(ErrorCode.QQ_QUERY_LIMITED);
        }
        QqInfoVO info = new QqInfoVO();
        info.setQq(qq);
        if (StringUtils.hasText(qqAvatarUrl)) {
            info.setAvatar(qqAvatarUrl.replace("{qq}", qq));
        }
        info.setEmail(qq + "@qq.com");
        fillFromProvider(qq, info);
        return info;
    }

    private void assertIpInterval(String ip) {
        String key = StringUtils.hasText(ip) ? ip : "unknown";
        long now = System.currentTimeMillis();
        AtomicBoolean allowed = new AtomicBoolean(false);
        lastQueryByIp.compute(key, (ignored, prev) -> {
            if (prev != null && now - prev < IP_INTERVAL_MS) {
                return prev;
            }
            allowed.set(true);
            return now;
        });
        pruneStaleQueries(now);
        if (!allowed.get()) {
            throw new BizException(ErrorCode.QQ_QUERY_TOO_FREQUENT);
        }
    }

    private void pruneStaleQueries(long now) {
        if (lastQueryByIp.size() < 512) {
            return;
        }
        long cutoff = now - IP_INTERVAL_MS;
        lastQueryByIp.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }

    private void fillFromProvider(String qq, QqInfoVO info) {
        if (!StringUtils.hasText(qqInfoKey) || !StringUtils.hasText(qqUserinfoUrl)) {
            log.warn("QQ 资料查询未配置完整，跳过");
            return;
        }
        try {
            URI uri = UriComponentsBuilder.fromUriString(qqUserinfoUrl.trim())
                    .queryParam("key", qqInfoKey.trim())
                    .queryParam("qq", qq)
                    .encode()
                    .build()
                    .toUri();
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
            applyProfile(qq, info, response.getBody());
        } catch (Exception e) {
            log.warn("查询 QQ 资料失败", e);
            logService.recordFail("查询QQ信息", "qq=" + qq, e);
        }
    }

    private void applyProfile(String qq, QqInfoVO info, String body) {
        if (!StringUtils.hasText(body)) {
            logService.recordFail("查询QQ信息", "qq=" + qq + " 响应为空");
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.path("code").asInt(-1) != 200) {
                String message = text(root, "msg");
                logService.recordFail("查询QQ信息", "qq=" + qq + (message == null ? " 查询失败" : " " + message));
                return;
            }
            JsonNode data = root.path("data");
            if (!data.isObject()) {
                logService.recordFail("查询QQ信息", "qq=" + qq + " 响应为空");
                return;
            }
            String nickname = text(data, "name");
            if (!StringUtils.hasText(nickname)) {
                nickname = text(data.path("data"), "sNickName");
            }
            if (StringUtils.hasText(nickname)) {
                info.setNickname(nickname.length() > 20 ? nickname.substring(0, 20) : nickname);
            }
            String avatar = text(data, "avatar");
            if (!StringUtils.hasText(avatar)) {
                avatar = text(data.path("data"), "sFaceUrl");
            }
            if (StringUtils.hasText(avatar)) {
                info.setAvatar(avatar.startsWith("http://") ? "https://" + avatar.substring(7) : avatar);
            }
        } catch (Exception e) {
            log.warn("解析 QQ 资料失败");
            logService.recordFail("查询QQ信息", "qq=" + qq + " 响应无法解析");
        }
    }

    private static String text(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        return StringUtils.hasText(value) ? value : null;
    }
}
