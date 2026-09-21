package com.blog.service;

import com.blog.common.IdGenerator;
import com.blog.entity.Music;
import com.blog.mapper.MusicMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class MusicService {
    @Resource
    private MusicMapper musicMapper;

    public List<Music> list() {
        return musicMapper.selectAll();
    }

    public List<Music> frontList() {
        return musicMapper.selectVisible();
    }

    public Long save(Music music) {
        music.setName(music.getName().trim());
        music.setAuthor(trimToNull(music.getAuthor()));
        music.setUrl(music.getUrl().trim());
        music.setCover(trimToNull(music.getCover()));
        music.setLrc(trimToNull(music.getLrc()));
        if (music.getStatus() == null) {
            music.setStatus(1);
        }
        if (music.getId() == null) {
            music.setId(IdGenerator.nextId());
            musicMapper.insert(music);
        } else {
            musicMapper.update(music);
        }
        return music.getId();
    }

    public void delete(Long id) {
        musicMapper.deleteById(id);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
