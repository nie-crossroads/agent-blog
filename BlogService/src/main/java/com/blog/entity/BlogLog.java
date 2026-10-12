package com.blog.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogLog {
    private Integer id;
    private String event;
    private String status;
    private Integer exeTime;
    private String detail;
    /** 调用来源：0 前台，1 后台 */
    private Integer consumer;
    private LocalDateTime createTime;
}
