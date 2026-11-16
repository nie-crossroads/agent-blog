package com.blog.controller;

import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.dto.CadenzaMusicRequest;
import com.blog.entity.Music;
import com.blog.service.MusicService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/cadenza")
public class CadenzaMusicController {
    @Resource
    private MusicService musicService;

    @Value("${blog.cadenza.token:}")
    private String token;

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
        String expected = token == null ? "" : token.trim();
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String presented = "";
        if (header != null && header.startsWith("Bearer ")) {
            presented = header.substring(7).trim();
        }
        if (!StringUtils.hasText(expected) || !same(expected, presented)) {
            throw new BizException(ErrorCode.CADENZA_TOKEN_INVALID);
        }
    }

    private static boolean same(String expected, String presented) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }
}
