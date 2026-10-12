package com.blog.controller.front;

import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.PageQuery;
import com.blog.common.PageResult;
import com.blog.dto.CommentSubmitRequest;
import com.blog.dto.MessageSubmitRequest;
import com.blog.dto.QqInfoVO;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.Comment;
import com.blog.entity.Essay;
import com.blog.entity.Friend;
import com.blog.entity.FriendCategory;
import com.blog.entity.Message;
import com.blog.entity.Music;
import com.blog.entity.Record;
import com.blog.entity.RecordCategory;
import com.blog.entity.Tag;
import com.blog.entity.WebUpdateLog;
import com.blog.service.ArticleService;
import com.blog.service.CommentService;
import com.blog.service.EssayService;
import com.blog.service.FriendService;
import com.blog.service.MessageService;
import com.blog.service.MetaService;
import com.blog.service.MusicService;
import com.blog.service.QqInfoService;
import com.blog.service.RecordService;
import com.blog.service.WebUpdateLogService;
import com.blog.validation.FrontVisitor;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.groups.Default;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/front")
public class FrontController {
    @Resource
    private ArticleService articleService;
    @Resource
    private MetaService metaService;
    @Resource
    private CommentService commentService;
    @Resource
    private MessageService messageService;
    @Resource
    private EssayService essayService;
    @Resource
    private RecordService recordService;
    @Resource
    private FriendService friendService;
    @Resource
    private MusicService musicService;
    @Resource
    private WebUpdateLogService webUpdateLogService;
    @Resource
    private QqInfoService qqInfoService;

    @GetMapping("/articles")
    public ResponseEntity<PageResult<Article>> articles(PageQuery query) {
        query.setStatus(1);
        return ResponseEntity.ok(articleService.page(query));
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<Article> article(@PathVariable Long id) {
        Article article = articleService.detail(id, true);
        if (!Integer.valueOf(1).equals(article.getStatus())) {
            throw new BizException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        return ResponseEntity.ok(article);
    }

    @GetMapping("/articles/recommend")
    public ResponseEntity<List<Article>> recommendArticles() {
        return ResponseEntity.ok(articleService.recommend(5));
    }

    @GetMapping("/articles/archive")
    public ResponseEntity<List<Article>> archive() {
        return ResponseEntity.ok(articleService.archive());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> categories() {
        return ResponseEntity.ok(metaService.categories());
    }

    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> tags() {
        return ResponseEntity.ok(metaService.tags());
    }

    @GetMapping("/sidebar")
    public ResponseEntity<Map<String, Object>> sidebar() {
        Map<String, Object> data = new HashMap<>();
        data.put("hotArticles", articleService.hot(5));
        data.put("categories", metaService.categories());
        data.put("tags", metaService.tags());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/qq-info")
    public ResponseEntity<QqInfoVO> qqInfo(String qq, HttpServletRequest request) {
        return ResponseEntity.ok(qqInfoService.lookup(qq, request));
    }

    @GetMapping("/comments")
    public ResponseEntity<List<Comment>> comments(Long articleId) {
        return ResponseEntity.ok(commentService.treeByArticle(articleId));
    }

    @PostMapping("/comments")
    public ResponseEntity<Void> submitComment(@Validated({Default.class, FrontVisitor.class}) @RequestBody CommentSubmitRequest req, HttpServletRequest request) {
        commentService.submit(req, request, false);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/messages")
    public ResponseEntity<PageResult<Message>> messages(Integer pageId, PageQuery query) {
        return ResponseEntity.ok(messageService.treeVisiblePage(pageId, query));
    }

    @PostMapping("/messages")
    public ResponseEntity<Void> submitMessage(@Validated({Default.class, FrontVisitor.class}) @RequestBody MessageSubmitRequest req, HttpServletRequest request) {
        messageService.submit(req, request, false);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/essays")
    public ResponseEntity<PageResult<Essay>> essays(PageQuery query) {
        query.setStatus(1);
        return ResponseEntity.ok(essayService.page(query));
    }

    @GetMapping("/records")
    public ResponseEntity<PageResult<Record>> records(PageQuery query) {
        query.setStatus(1);
        return ResponseEntity.ok(recordService.page(query));
    }

    @GetMapping("/record-categories")
    public ResponseEntity<List<RecordCategory>> recordCategories() {
        return ResponseEntity.ok(recordService.categories());
    }

    @GetMapping("/friends")
    public ResponseEntity<List<Friend>> friends() {
        return ResponseEntity.ok(friendService.friends());
    }

    @GetMapping("/friend-categories")
    public ResponseEntity<List<FriendCategory>> friendCategories() {
        return ResponseEntity.ok(friendService.friendCategories());
    }

    @GetMapping("/music")
    public ResponseEntity<List<Music>> music() {
        return ResponseEntity.ok(musicService.frontList());
    }

    @GetMapping("/web-update-logs")
    public ResponseEntity<List<WebUpdateLog>> webUpdateLogs() {
        return ResponseEntity.ok(webUpdateLogService.list());
    }
}
