package com.blog.service;

import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.IdGenerator;
import com.blog.common.PageQuery;
import com.blog.common.PageResult;
import com.blog.dto.CommentSubmitRequest;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.blog.common.RequestUserAgent.*;

@Service
public class CommentService {
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private IpLocationService ipLocationService;
    @Resource
    private FrontReplyLimitService frontReplyLimitService;
    @Resource
    private MailNotificationService mailNotificationService;
    @Value("${spring.mail.username}")
    private String mailUsername;
    @Value("${blog.site.title}")
    private String siteTitle;
    @Value("${blog.site.msg-avatar}")
    private String msgAvatar;

    public List<Comment> treeByArticle(Long articleId) {
        return toTree(commentMapper.selectByArticleId(articleId));
    }

    public PageResult<Comment> page(PageQuery query) {
        return new PageResult<>(commentMapper.countPage(query), commentMapper.selectPage(query));
    }

    public void submit(CommentSubmitRequest req, HttpServletRequest request, boolean asBlogger) {
        Article article = articleMapper.selectById(req.getArticleId());
        if (article == null) {
            throw new BizException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        if (!asBlogger && !Integer.valueOf(1).equals(article.getComment())) {
            throw new BizException(ErrorCode.COMMENT_CLOSED);
        }
        String ip = clientIp(request);
        if (!asBlogger) {
            frontReplyLimitService.assertVisitorAllowed(request, req.getNickname(), req.getEmail());
        }
        Comment comment = new Comment();
        comment.setId(IdGenerator.nextId());
        comment.setArticleId(req.getArticleId());
        comment.setContent(req.getContent());
        comment.setBlogger(asBlogger ? 1 : 0);
        Comment parent = fillReplyMeta(comment, req.getParentId());
        comment.setNickname(asBlogger ? siteTitle : trim(req.getNickname(), 20));
        comment.setEmail(trim(asBlogger && !StringUtils.hasText(req.getEmail()) ? mailUsername : req.getEmail(), 30));
        comment.setWebsite(trim(req.getWebsite(), 50));
        comment.setAvatar(asBlogger ? msgAvatar : trim(req.getAvatar(), 255));
        comment.setHandle(asBlogger ? 1 : 0);
        comment.setNotice(req.getNotice() == null ? 0 : req.getNotice());
        comment.setSend(0);
        comment.setVisible(1);
        String ua = request.getHeader("User-Agent");
        comment.setBrowser(trim(parseBrowser(ua), 50));
        comment.setSystemInfo(trim(parseOs(request), 20));
        comment.setIp(trim(ip, 15));
        IpLocationService.Location location = ipLocationService.lookup(ip);
        comment.setProvince(location.province());
        comment.setCity(location.city());
        comment.setDistrict(location.district());
        commentMapper.insert(comment);
        if (frontVisible(comment.getHandle(), comment.getVisible())) {
            articleMapper.incrementComments(req.getArticleId(), 1);
        }
        if (asBlogger && req.getParentId() != null) {
            commentMapper.updateHandle(req.getParentId(), 1);
            if (parent != null && !frontVisible(parent.getHandle(), parent.getVisible())
                    && Integer.valueOf(1).equals(parent.getVisible())) {
                articleMapper.incrementComments(req.getArticleId(), 1);
            }
            if (parent != null) {
                parent.setHandle(1);
                mailNotificationService.notifyCommentReply(parent);
            }
            mailNotificationService.notifyCommentReply(comment);
        }
    }

    public void handle(Long id, Integer handle) {
        commentMapper.updateHandle(id, handle);
    }

    public void review(Long id, boolean approved) {
        Comment old = commentMapper.selectById(id);
        if (old == null) {
            throw new BizException(ErrorCode.COMMENT_PARENT_NOT_FOUND);
        }
        int visible = approved ? 1 : 0;
        boolean wasShown = frontVisible(old.getHandle(), old.getVisible());
        commentMapper.updateReview(id, visible);
        old.setHandle(1);
        old.setVisible(visible);
        boolean nowShown = approved;
        if (old.getArticleId() != null && wasShown != nowShown) {
            articleMapper.incrementComments(old.getArticleId(), nowShown ? 1 : -1);
        }
        if (approved) {
            mailNotificationService.notifyCommentReply(old);
        }
    }

    public void visible(Long id, Integer visible) {
        Comment old = commentMapper.selectById(id);
        commentMapper.updateVisible(id, visible);
        if (old == null || old.getArticleId() == null || visible == null || visible.equals(old.getVisible())
                || !Integer.valueOf(1).equals(old.getHandle())) {
            return;
        }
        articleMapper.incrementComments(old.getArticleId(), Integer.valueOf(1).equals(visible) ? 1 : -1);
    }

    public void delete(Long id) {
        visible(id, 0);
    }

    private Comment fillReplyMeta(Comment comment, Long parentId) {
        if (parentId == null) {
            comment.setRootId(comment.getId());
            return null;
        }
        Comment parent = commentMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException(ErrorCode.COMMENT_PARENT_NOT_FOUND);
        }
        comment.setParentId(parent.getId());
        comment.setParentNickname(parent.getNickname());
        comment.setRootId(parent.getRootId() != null ? parent.getRootId() : parent.getId());
        return parent;
    }

    private List<Comment> toTree(List<Comment> list) {
        Map<Long, Comment> map = new LinkedHashMap<>();
        for (Comment c : list) {
            c.setChildren(new ArrayList<>());
            map.put(c.getId(), c);
        }
        List<Comment> roots = new ArrayList<>();
        for (Comment c : list) {
            if (isTopLevel(c.getParentId())) {
                roots.add(c);
                continue;
            }
            Comment parent = map.get(c.getParentId());
            if (parent == null) {
                continue;
            }
            Comment root = map.get(c.getRootId());
            if (root == null || !isTopLevel(root.getParentId())) {
                root = parent;
                while (root != null && !isTopLevel(root.getParentId()) && map.containsKey(root.getParentId())) {
                    root = map.get(root.getParentId());
                }
            }
            if (root != null && !root.getId().equals(c.getId())) {
                root.getChildren().add(c);
            }
        }
        return roots;
    }

    static boolean isTopLevel(Long parentId) {
        return parentId == null || parentId == 0L;
    }
}
