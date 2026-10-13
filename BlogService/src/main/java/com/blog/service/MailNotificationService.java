package com.blog.service;

import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.PageQuery;
import com.blog.common.PageResult;
import com.blog.entity.Comment;
import com.blog.entity.EmailFail;
import com.blog.entity.EmailRecord;
import com.blog.entity.Message;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.EmailFailMapper;
import com.blog.mapper.EmailRecordMapper;
import com.blog.mapper.MessageMapper;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.RejectedExecutionException;

@Service
public class MailNotificationService {
    static final String KIND_COMMENT = "comment";
    static final String KIND_MESSAGE = "message";

    @Resource
    private JavaMailSender javaMailSender;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private MessageMapper messageMapper;
    @Resource
    private EmailRecordMapper emailRecordMapper;
    @Resource
    private EmailFailMapper emailFailMapper;
    @Resource
    private LogService logService;
    @Resource(name = "mailExecutor")
    private ThreadPoolTaskExecutor mailExecutor;

    @Value("${spring.mail.username}")
    private String mailUsername;
    @Value("${spring.mail.password}")
    private String mailPassword;
    @Value("${blog.site.site-url}")
    private String siteUrl;
    @Value("${blog.site.title}")
    private String siteTitle;

    public PageResult<EmailFail> failPage(PageQuery query) {
        return new PageResult<>(emailFailMapper.countPage(query), emailFailMapper.selectPage(query));
    }

    public PageResult<EmailRecord> page(PageQuery query) {
        return new PageResult<>(emailRecordMapper.countPage(query), emailRecordMapper.selectPage(query));
    }

    public void notifyCommentReply(Comment reply) {
        if (reply == null || !shouldNotify(reply.getParentId(), reply.getHandle(), reply.getSend(), reply.getVisible())) {
            return;
        }
        Comment parent = commentMapper.selectById(reply.getParentId());
        if (parent == null) {
            logService.recordFail("评论回复邮件", "回复记录 " + reply.getId() + " 的收件人不存在");
            return;
        }
        if (Integer.valueOf(1).equals(parent.getBlogger())
                || !Integer.valueOf(1).equals(parent.getNotice())
                || !StringUtils.hasText(parent.getEmail())) {
            return;
        }
        enqueue(job(
                KIND_COMMENT,
                "评论回复邮件",
                reply.getId(),
                parent.getId(),
                reply.getArticleId(),
                null,
                displayName(reply.getBlogger(), reply.getNickname()),
                reply.getEmail(),
                reply.getContent(),
                parent.getNickname(),
                parent.getEmail(),
                parent.getContent(),
                normalizedSiteUrl() + "/article/" + reply.getArticleId()
        ));
    }

    public void notifyMessageReply(Message reply) {
        if (reply == null || !shouldNotify(reply.getParentId(), reply.getHandle(), reply.getSend(), reply.getVisible())) {
            return;
        }
        Message parent = messageMapper.selectById(reply.getParentId());
        if (parent == null) {
            logService.recordFail("留言回复邮件", "回复记录 " + reply.getId() + " 的收件人不存在");
            return;
        }
        if (Integer.valueOf(1).equals(parent.getBlogger())
                || !Integer.valueOf(1).equals(parent.getNotice())
                || !StringUtils.hasText(parent.getEmail())) {
            return;
        }
        String pagePath = Integer.valueOf(36).equals(reply.getPageId()) ? "/friends" : "/messages";
        enqueue(job(
                KIND_MESSAGE,
                "留言回复邮件",
                reply.getId(),
                parent.getId(),
                null,
                reply.getPageId(),
                displayName(reply.getBlogger(), reply.getNickname()),
                reply.getEmail(),
                reply.getContent(),
                parent.getNickname(),
                parent.getEmail(),
                parent.getContent(),
                normalizedSiteUrl() + pagePath
        ));
    }

    public void resend(Integer id) {
        EmailFail fail = emailFailMapper.selectById(id);
        if (fail == null) {
            throw new BizException(ErrorCode.EMAIL_FAIL_NOT_FOUND);
        }
        MailJob job = job(
                fail.getKind(),
                KIND_COMMENT.equals(fail.getKind()) ? "评论回复邮件" : "留言回复邮件",
                fail.getReplyId(),
                fail.getParentId(),
                fail.getArticleId(),
                fail.getPageId(),
                fail.getSendName(),
                fail.getSendEmail(),
                fail.getContent(),
                fail.getReceiveName(),
                fail.getReceiveEmail(),
                fail.getOriginalContent(),
                fail.getLink()
        );
        try {
            deliver(job, fail.getId());
        } catch (Exception error) {
            logService.recordFail(job.event, "回复记录 " + job.replyId + " 重发失败", error);
            emailFailMapper.updateExtra(fail.getId(), clip(LogService.describe(error), 255));
            throw new BizException(ErrorCode.EMAIL_RESEND_FAILED);
        }
    }

    private void enqueue(MailJob job) {
        try {
            mailExecutor.execute(() -> {
                try {
                    deliver(job, null);
                } catch (Exception error) {
                    recordFailure(job, error);
                }
            });
        } catch (RejectedExecutionException error) {
            recordFailure(job, error);
        }
    }

    private void deliver(MailJob job, Integer failId) throws Exception {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
        helper.setFrom(mailUsername, siteTitle);
        helper.setTo(job.receiveEmail);
        helper.setSubject("Crossroads：" + oneLine(job.replyName) + " 回复了你");
        helper.setText(html(job.receiveName, job.replyName, job.originalContent, job.replyContent, job.link), true);
        javaMailSender.send(mimeMessage);
        markSent(job);
        emailRecordMapper.insert(toRecord(job));
        if (failId != null) {
            emailFailMapper.deleteById(failId);
        }
    }

    private void markSent(MailJob job) {
        if (KIND_COMMENT.equals(job.kind)) {
            commentMapper.updateSend(job.replyId);
            commentMapper.updateSend(job.parentId);
            return;
        }
        messageMapper.updateSend(job.replyId);
        messageMapper.updateSend(job.parentId);
    }

    private void recordFailure(MailJob job, Exception error) {
        logService.recordFail(job.event, "回复记录 " + job.replyId + " 发送失败", error);
        EmailFail fail = new EmailFail();
        fail.setKind(job.kind);
        fail.setReplyId(job.replyId);
        fail.setParentId(job.parentId);
        fail.setArticleId(job.articleId);
        fail.setPageId(job.pageId);
        fail.setSendName(clip(job.replyName, 20));
        fail.setSendEmail(clip(job.replyEmail, 25));
        fail.setReceiveName(clip(job.receiveName, 20));
        fail.setReceiveEmail(clip(job.receiveEmail, 25));
        fail.setContent(clip(job.replyContent, 255));
        fail.setOriginalContent(clip(job.originalContent, 255));
        fail.setLink(clip(job.link, 255));
        fail.setExtra(clip(LogService.describe(error), 255));
        try {
            emailFailMapper.insert(fail);
        } catch (Exception insertError) {
            logService.recordFail(job.event, "写入失败邮件记录失败", insertError);
        }
    }

    private EmailRecord toRecord(MailJob job) {
        EmailRecord record = new EmailRecord();
        record.setArticleId(job.articleId);
        record.setPageId(job.pageId);
        record.setMessageId(job.replyId);
        record.setSendName(clip(job.replyName, 20));
        record.setSendEmail(clip(job.replyEmail, 25));
        record.setReceiveName(clip(job.receiveName, 20));
        record.setReceiveEmail(clip(job.receiveEmail, 25));
        record.setContent(clip(job.replyContent, 255));
        return record;
    }

    private MailJob job(
            String kind,
            String event,
            Long replyId,
            Long parentId,
            Long articleId,
            Integer pageId,
            String replyName,
            String replyEmail,
            String replyContent,
            String receiveName,
            String receiveEmail,
            String originalContent,
            String link
    ) {
        MailJob job = new MailJob();
        job.kind = kind;
        job.event = event;
        job.replyId = replyId;
        job.parentId = parentId;
        job.articleId = articleId;
        job.pageId = pageId;
        job.replyName = replyName;
        job.replyEmail = replyEmail;
        job.replyContent = replyContent;
        job.receiveName = receiveName;
        job.receiveEmail = receiveEmail;
        job.originalContent = originalContent;
        job.link = link;
        return job;
    }

    private String displayName(Integer blogger, String nickname) {
        return Integer.valueOf(1).equals(blogger) ? siteTitle : nickname;
    }

    private static boolean shouldNotify(Long parentId, Integer handle, Integer send, Integer visible) {
        return parentId != null
                && parentId != 0L
                && Integer.valueOf(1).equals(handle)
                && Integer.valueOf(1).equals(visible)
                && !Integer.valueOf(1).equals(send);
    }

    private String normalizedSiteUrl() {
        String value = siteUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String html(String receiveName, String replyName, String original, String reply, String link) {
        return "<p>" + escape(receiveName) + "，您好：</p>"
                + "<p><strong>" + escape(replyName) + "</strong> 回复了您：</p>"
                + "<p>原内容：</p><blockquote>" + escape(original) + "</blockquote>"
                + "<p>回复内容：</p><blockquote>" + escape(reply) + "</blockquote>"
                + "<p><a href=\"" + escape(link) + "\">前往 Crossroads 查看</a></p>";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\r\n", "<br>")
                .replace("\n", "<br>")
                .replace("\r", "<br>");
    }

    private static String oneLine(String value) {
        if (value == null) {
            return "有人";
        }
        String text = value.replaceAll("[\\r\\n]+", " ").trim();
        return text.isEmpty() ? "有人" : text;
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    private static class MailJob {
        private String kind;
        private String event;
        private Long replyId;
        private Long parentId;
        private Long articleId;
        private Integer pageId;
        private String replyName;
        private String replyEmail;
        private String replyContent;
        private String receiveName;
        private String receiveEmail;
        private String originalContent;
        private String link;
    }
}
