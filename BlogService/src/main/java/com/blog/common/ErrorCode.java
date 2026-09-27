package com.blog.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "323201", "用户名或密码错误"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "323202", "账号已被禁用"),
    CURRENT_PASSWORD_WRONG(HttpStatus.BAD_REQUEST, "323203", "当前密码错误"),
    PLEASE_LOGIN(HttpStatus.UNAUTHORIZED, "323204", "用户还未登录，请先进行登录"),
    USERNAME_EMPTY(HttpStatus.BAD_REQUEST, "323205", "用户名不能为空"),
    USERNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "323206", "用户名不能超过15个字符"),
    USERNAME_UNCHANGED(HttpStatus.BAD_REQUEST, "323207", "用户名未修改"),
    USERNAME_EXISTS(HttpStatus.CONFLICT, "323208", "用户名已存在"),
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "323209", "请填写密码"),
    PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "323210", "新密码至少6位"),
    PASSWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "323211", "新密码过长"),
    PASSWORD_SAME_AS_OLD(HttpStatus.BAD_REQUEST, "323212", "新密码不能与当前密码相同"),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "323213", "账号不存在"),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "323214", "请求体不是合法 JSON"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.BAD_REQUEST, "323215", "请使用 Content-Type: application/json"),
    PARAM_INVALID(HttpStatus.BAD_REQUEST, "323216", "请求参数不正确"),
    NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "323217", "请填写昵称"),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "323218", "昵称不能超过20个字符"),
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "323248", "请填写邮箱"),
    EMAIL_INVALID(HttpStatus.BAD_REQUEST, "323219", "请填写正确的邮箱格式"),
    EMAIL_TOO_LONG(HttpStatus.BAD_REQUEST, "323220", "邮箱不能超过30个字符"),
    CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "323221", "请填写内容"),
    CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "323222", "消息内容不能超过255个字符"),
    TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "323223", "标题不能为空"),
    FILE_REQUIRED(HttpStatus.BAD_REQUEST, "323224", "请选择文件"),
    MUSIC_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "323225", "请填写歌名"),
    MUSIC_URL_REQUIRED(HttpStatus.BAD_REQUEST, "323226", "请填写播放地址"),
    CATEGORY_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "323227", "分类名不能为空"),
    CATEGORY_NAME_EXISTS(HttpStatus.CONFLICT, "323228", "分类名已存在"),
    TAG_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "323229", "标签名不能为空"),
    TAG_NAME_EXISTS(HttpStatus.CONFLICT, "323230", "标签名已存在"),
    ILLEGAL_FILE_PATH(HttpStatus.BAD_REQUEST, "323231", "非法文件路径"),
    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "323232", "文章不存在"),
    ARTICLE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "323233", "请指定文章"),
    COMMENT_CLOSED(HttpStatus.FORBIDDEN, "323234", "该文章未开放评论"),
    USER_BLACKLISTED(HttpStatus.FORBIDDEN, "323235", "已被限制发言，每天只能发言5次(●'◡'●)"),
    COMMENT_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "323236", "回复的评论不存在"),
    MESSAGE_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "323237", "回复的留言不存在"),
    CATEGORY_HAS_ARTICLES(HttpStatus.CONFLICT, "323238", "该分类下仍有文章，无法删除"),
    TAG_HAS_ARTICLES(HttpStatus.CONFLICT, "323239", "该标签下仍有文章，无法删除"),
    CATEGORY_HAS_RECORDS(HttpStatus.CONFLICT, "323240", "该分类下仍有记录，无法删除"),
    CATEGORY_HAS_FRIENDS(HttpStatus.CONFLICT, "323241", "该分类下仍有友链，无法删除"),
    FILE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "323242", "读取上传文件失败"),
    FILE_WRITE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "323243", "文件保存失败"),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "323244", "文件删除失败"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "323245", "服务器内部错误"),
    POST_RATE_LIMITED(HttpStatus.FORBIDDEN, "323246", "今日发言次数已达上限，每天只能发言5次(●'◡'●)"),
    MESSAGE_PAGE_INVALID(HttpStatus.BAD_REQUEST, "323247", "留言页面无效"),
    EMAIL_BLOGGER_RESERVED(HttpStatus.BAD_REQUEST, "323249", "不能输入博主的邮箱"),
    QQ_QUERY_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "323250", "今日 QQ 信息查询次数已达上限"),
    QQ_QUERY_TOO_FREQUENT(HttpStatus.TOO_MANY_REQUESTS, "323251", "QQ 信息查询过于频繁，请稍后再试"),
    EMAIL_FAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "323252", "失败邮件记录不存在"),
    EMAIL_RESEND_FAILED(HttpStatus.BAD_GATEWAY, "323253", "邮件重发失败"),
    EVENT_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "323254", "请选择事件时间"),
    EVENT_DATE_INVALID(HttpStatus.BAD_REQUEST, "323255", "事件时间格式应为 2025-03-05"),
    FRONT_API_TOO_FREQUENT(HttpStatus.TOO_MANY_REQUESTS, "323256", "服务器繁忙，请稍后再试"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "323257", "公网扫描资源不存在"),

    COS_CONFIG_INCOMPLETE(HttpStatus.INTERNAL_SERVER_ERROR, "323301", "对象存储配置不完整"),
    COS_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "323302", "文件上传失败"),
    COS_DELETE_FAILED(HttpStatus.BAD_GATEWAY, "323303", "文件删除失败"),
    COS_UPLOAD_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "323304", "文件上传被中断"),

    QINIU_CONFIG_INCOMPLETE(HttpStatus.INTERNAL_SERVER_ERROR, "323401", "对象存储配置不完整"),
    QINIU_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "323402", "文件上传失败"),
    QINIU_DELETE_FAILED(HttpStatus.BAD_GATEWAY, "323403", "文件删除失败");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public static ErrorCode fromMessage(String message) {
        if (message == null || message.isBlank()) {
            return PARAM_INVALID;
        }
        for (ErrorCode value : values()) {
            if (value.message.equals(message)) {
                return value;
            }
        }
        return PARAM_INVALID;
    }
}
