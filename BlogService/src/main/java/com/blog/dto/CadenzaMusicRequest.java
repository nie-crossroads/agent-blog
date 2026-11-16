package com.blog.dto;

import com.blog.validation.RequiredText;
import lombok.Data;

@Data
public class CadenzaMusicRequest {
    @RequiredText(message = "请填写歌名", max = 20, tooLongMessage = "歌名不能超过20个字符")
    private String name;

    @RequiredText(required = false, max = 20, tooLongMessage = "作者不能超过20个字符")
    private String author;

    @RequiredText(message = "请填写播放地址", max = 80, tooLongMessage = "播放地址不能超过80个字符")
    private String url;

    @RequiredText(required = false, max = 100, tooLongMessage = "封面地址不能超过100个字符")
    private String cover;

    /** 0 前台不显示，1 前台显示；不传则显示 */
    private Integer status;
}
