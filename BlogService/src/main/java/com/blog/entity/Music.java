package com.blog.entity;

import com.blog.validation.RequiredText;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Music {
    private Long id;
    @RequiredText(message = "请填写歌名")
    private String name;
    private String author;
    @RequiredText(message = "请填写播放地址")
    private String url;
    private String cover;
    private String lrc;
    /** 0:前台不显示 1:前台显示 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
