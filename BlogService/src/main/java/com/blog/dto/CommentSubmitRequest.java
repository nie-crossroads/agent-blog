package com.blog.dto;

import com.blog.validation.FrontVisitor;
import com.blog.validation.RequiredText;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentSubmitRequest {
    @NotNull(message = "请指定文章")
    private Long articleId;
    private Long parentId;
    @RequiredText(message = "请填写内容", max = 255, tooLongMessage = "消息内容不能超过255个字符")
    private String content;
    @RequiredText(message = "请填写昵称", max = 20, tooLongMessage = "昵称不能超过20个字符")
    private String nickname;
    @RequiredText(
            required = false,
            message = "请填写邮箱",
            max = 30,
            tooLongMessage = "邮箱不能超过30个字符",
            pattern = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
            patternMessage = "请填写正确的邮箱格式",
            forbidden = "1762546812@qq.com",
            forbiddenMessage = "不能输入博主的邮箱",
            groups = FrontVisitor.class)
    private String email;
    private String website;
    private String avatar;
    private Integer notice;
}
