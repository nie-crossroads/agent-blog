package com.blog.controller;

import com.blog.cadenza.CadenzaTokens;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.dto.CadenzaMusicRequest;
import com.blog.entity.Music;
import com.blog.service.MusicService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;

@RestController
@RequestMapping("/api/cadenza")
public class CadenzaMusicController {
    private static final Logger log = LoggerFactory.getLogger(CadenzaMusicController.class);

    @Resource
    private MusicService musicService;

    @Value("${blog.cadenza.root-key}")
    private String rootKeyText;

    @Value("${blog.cadenza.revoked:}")
    private String revokedText;

    private byte[] rootKey = new byte[0];
    private Set<Integer> revoked = Set.of();

    @PostConstruct
    void initCadenza() {
        rootKey = CadenzaTokens.decodeRootKey(rootKeyText);
        revoked = CadenzaTokens.parseRevoked(revokedText);
        if (rootKey.length != 32) {
            log.warn("blog.cadenza.root-key 未配置或不是 32 字节，音乐平台接口将拒绝全部令牌");
        }
    }

    @PostMapping("/music")
    public ResponseEntity<Long> save(@Valid @RequestBody CadenzaMusicRequest request, HttpServletRequest http) {
        assertToken(http);
        Music music = new Music();
        music.setName(request.getName());
        music.setAuthor(request.getAuthor());
        music.setUrl(request.getUrl());
        music.setCover(request.getCover());
        music.setStatus(request.getStatus());
        return ResponseEntity.ok(musicService.saveFromCadenza(music));
    }

    private void assertToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String presented = "";
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            presented = header.substring(7).trim();
        }
        try {
            CadenzaTokens.verify(rootKey, presented, revoked, Instant.now().getEpochSecond());
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.CADENZA_TOKEN_INVALID);
        }
    }
}
