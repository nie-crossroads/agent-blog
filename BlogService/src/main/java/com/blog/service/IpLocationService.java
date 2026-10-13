package com.blog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class IpLocationService {
    private static final Logger log = LoggerFactory.getLogger(IpLocationService.class);
    private static final int DAILY_LIMIT = 1000;

    public record Location(String province, String city, String district) {
        static Location empty() {
            return new Location(null, null, null);
        }
    }

    @Value("${blog.map.qq-key}")
    private String qqKey;
    @Value("${blog.map.ip-url}")
    private String ipUrl;

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private LogService logService;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private DailyQuotaService dailyQuotaService;

    public Location lookup(String ip) {
        String queryIp = normalizeIp(ip);
        if (!StringUtils.hasText(queryIp) || isPrivate(queryIp)) {
            return Location.empty();
        }
        if (!dailyQuotaService.tryAcquireIp(DAILY_LIMIT)) {
            log.warn("地理位置查询已达每日上限");
            return Location.empty();
        }
        try {
            URI uri = UriComponentsBuilder.fromUriString(ipUrl.trim())
                    .queryParam("ip", queryIp)
                    .queryParam("key", qqKey)
                    .encode()
                    .build()
                    .toUri();
            String json = restTemplate.getForObject(uri, String.class);
            if (!StringUtils.hasText(json)) {
                return Location.empty();
            }
            JsonNode root = objectMapper.readTree(json);
            if (root.path("status").asInt(-1) != 0) {
                logService.recordFail("查询地理位置", "status=" + root.path("status").asInt()
                        + " message=" + root.path("message").asText(""));
                return Location.empty();
            }
            JsonNode info = root.path("result").path("ad_info");
            return new Location(
                    text(info, "province"),
                    text(info, "city"),
                    text(info, "district")
            );
        } catch (Exception e) {
            log.warn("查询地理位置失败", e);
            logService.recordFail("查询地理位置", "ip=" + queryIp, e);
            return Location.empty();
        }
    }

    private static String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return null;
        }
        String value = ip.trim();
        if (value.startsWith("::ffff:")) {
            value = value.substring(7);
        }
        return value;
    }

    private static boolean isPrivate(String ip) {
        String lower = ip.toLowerCase();
        return "127.0.0.1".equals(ip)
                || "localhost".equals(lower)
                || "::1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip)
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("169.254.")
                || ip.startsWith("fe80:")
                || isCarrierGradeOrPrivate172(ip);
    }

    private static boolean isCarrierGradeOrPrivate172(String ip) {
        if (!ip.startsWith("172.")) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException e) {
            log.warn("解析 IP 失败");
            return false;
        }
    }

    private static String text(JsonNode info, String field) {
        String value = info.path(field).asText(null);
        if (!StringUtils.hasText(value) || "未知".equals(value)) {
            return null;
        }
        return value.length() > 30 ? value.substring(0, 30) : value;
    }
}
