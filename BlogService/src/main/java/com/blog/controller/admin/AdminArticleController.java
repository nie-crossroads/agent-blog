package com.blog.controller.admin;

import com.blog.common.PageQuery;
import com.blog.common.PageResult;
import com.blog.dto.ArticleSaveRequest;
import com.blog.dto.CommentSubmitRequest;
import com.blog.dto.ReviewRequest;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.Comment;
import com.blog.entity.Tag;
import com.blog.service.ArticleService;
import com.blog.service.CommentService;
import com.blog.service.MetaService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminArticleController {
    @Resource
    private ArticleService articleService;
    @Resource
    private MetaService metaService;
    @Resource
    private CommentService commentService;

    @GetMapping("/articles")
    public ResponseEntity<PageResult<Article>> articles(PageQuery query) {
        return ResponseEntity.ok(articleService.page(query));
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<Article> article(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.detail(id, false));
    }

    @GetMapping("/articles/{id}/simple")
    public ResponseEntity<Article> articleSimple(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.meta(id));
    }

    @PostMapping("/articles")
    public ResponseEntity<Long> createArticle(@Valid @RequestBody ArticleSaveRequest req) {
        req.setId(null);
        return ResponseEntity.ok(articleService.save(req));
    }

    @PutMapping("/articles/{id}")
    public ResponseEntity<Long> updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleSaveRequest req) {
        req.setId(id);
        return ResponseEntity.ok(articleService.save(req));
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> categories() {
        return ResponseEntity.ok(metaService.categories());
    }

    @PostMapping("/categories")
    public ResponseEntity<Long> saveCategory(@Valid @RequestBody Category category) {
        return ResponseEntity.ok(metaService.saveCategory(category));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        metaService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> tags() {
        return ResponseEntity.ok(metaService.tags());
    }

    @PostMapping("/tags")
    public ResponseEntity<Long> saveTag(@Valid @RequestBody Tag tag) {
        return ResponseEntity.ok(metaService.saveTag(tag));
    }

    @DeleteMapping("/tags/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        metaService.deleteTag(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/comments")
    public ResponseEntity<PageResult<Comment>> comments(PageQuery query) {
        return ResponseEntity.ok(commentService.page(query));
    }

    @PostMapping("/comments")
    public ResponseEntity<Void> replyComment(@Valid @RequestBody CommentSubmitRequest req, HttpServletRequest request) {
        commentService.submit(req, request, true);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/comments/{id}/handle")
    public ResponseEntity<Void> handleComment(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        commentService.handle(id, body.get("handle"));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/comments/{id}/review")
    public ResponseEntity<Void> reviewComment(@PathVariable Long id, @Valid @RequestBody ReviewRequest req) {
        commentService.review(id, Boolean.TRUE.equals(req.getApproved()));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/comments/{id}/visible")
    public ResponseEntity<Void> visibleComment(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        commentService.visible(id, body.get("visible"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.delete(id);
        return ResponseEntity.ok().build();
    }
}
