package com.blog.dto;

import com.blog.validation.RequiredText;
import lombok.Data;

import java.util.List;

@Data
public class ArticleSaveRequest {
    private Long id;
    private Long categoryId;
    @RequiredText(message = "标题不能为空")
    private String title;
    private String description;
    private String cover;
    private String thumbnail;
    private Integer comment;
    private Integer status;
    private Integer recommend;
    private String content;
    private boolean simple;
    private List<Long> tagIds;
    private List<String> imageUrls;
    private List<ImageSaveItem> images;
}
