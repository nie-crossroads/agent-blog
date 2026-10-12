package com.blog.mapper;

import com.blog.common.PageQuery;
import com.blog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleMapper {
    List<Article> selectPage(PageQuery query);

    long countPage(PageQuery query);

    Article selectById(@Param("id") Long id);

    int insert(Article article);

    int update(Article article);

    int deleteById(@Param("id") Long id);

    int incrementPv(@Param("id") Long id);

    int incrementComments(@Param("id") Long id, @Param("delta") int delta);

    Integer sumPv();

    long countAll();

    long countPublished();

    List<Map<String, Object>> countByCategory();

    List<Article> selectRecent(@Param("limit") int limit);

    List<Article> selectHot(@Param("limit") int limit);

    List<Article> selectRecommend(@Param("limit") int limit);

    Article selectMetaById(@Param("id") Long id);

    List<Article> selectArchive();

    int recountCategory(@Param("categoryId") Long categoryId);

    int countByCategoryId(@Param("categoryId") Long categoryId);
}
