package com.simpleblog.service;

import com.simpleblog.model.dto.BlogInput;
import com.simpleblog.model.dto.BlogPage;
import com.simpleblog.model.dto.BlogSearchPage;
import com.simpleblog.model.entity.Blog;

import java.util.List;

public interface BlogService {
    /**
     * 分页查询博客列表
     * @param page 页码
     * @param size 每页大小
     * @param publishedOnly 是否只查询已发布的
     * @return 博客分页结果
     */
    BlogPage listBlogs(int page, int size, boolean publishedOnly);

    /**
     * 根据ID查找博客
     * @param id 博客ID
     * @return 博客对象
     */
    Blog findById(Long id);

    /**
     * 查询热门博客
     * @param limit 限制数量
     * @return 热门博客列表
     */
    List<Blog> listHotBlogs(int limit);

    /**
     * 搜索博客(使用数据库搜索)
     * @param query 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 博客分页结果
     */
    BlogSearchPage searchBlogs(String query, int page, int size);

    /**
     * 搜索博客(使用Elasticsearch)
     * @param query 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 博客分页结果
     */
    BlogSearchPage searchBlogsEs(String query, int page, int size);

    /**
     * 创建博客
     * @param authorId 作者ID
     * @param input 博客输入
     * @return 创建的博客
     */
    Blog createBlog(Long authorId, BlogInput input);

    /**
     * 更新博客
     * @param id 博客ID
     * @param input 博客输入
     * @return 更新的博客
     */
    Blog updateBlog(Long id, BlogInput input);

    /**
     * 删除博客
     * @param id 博客ID
     * @return 是否成功
     */
    boolean deleteBlog(Long id);

    /**
     * 投票
     * @param blogId 博客ID
     * @param value 投票值(1或-1)
     * @param userId 用户ID
     * @param ip IP地址
     * @return 更新后的博客
     */
    Blog voteBlog(Long blogId, int value, Long userId, String ip);

    /**
     * 获取用户投票
     * @param blogId 博客ID
     * @param userId 用户ID
     * @param ip IP地址
     * @return 投票值
     */
    Integer getUserVote(Long blogId, Long userId, String ip);

    /**
     * 获取点赞数
     * @param blogId 博客ID
     * @param fallback 回退值
     * @return 点赞数
     */
    long getLikes(Long blogId, long fallback);

    /**
     * 获取点踩数
     * @param blogId 博客ID
     * @param fallback 回退值
     * @return 点踩数
     */
    long getDislikes(Long blogId, long fallback);

    /**
     * 查找博客关联的标签ID
     * @param blogId 博客ID
     * @return 标签ID列表
     */
    List<Long> findTagIds(Long blogId);
}
