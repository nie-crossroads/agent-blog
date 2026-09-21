package com.blog.mapper;

import com.blog.entity.Music;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MusicMapper {
    List<Music> selectAll();

    Music selectById(@Param("id") Long id);

    List<Music> selectVisible();

    int insert(Music music);

    int update(Music music);

    int deleteById(@Param("id") Long id);
}
