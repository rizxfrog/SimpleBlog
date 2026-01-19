package com.simpleblog.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simpleblog.mapper.BlogMapper;
import com.simpleblog.mapper.BlogTagMapper;
import com.simpleblog.model.dto.BlogInput;
import com.simpleblog.model.dto.BlogPage;
import com.simpleblog.model.entity.Blog;
import com.simpleblog.model.entity.BlogTag;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BlogService {
    private final BlogMapper blogMapper;
    private final BlogTagMapper blogTagMapper;

    public BlogService(BlogMapper blogMapper, BlogTagMapper blogTagMapper) {
        this.blogMapper = blogMapper;
        this.blogTagMapper = blogTagMapper;
    }

    public BlogPage listBlogs(int page, int size, boolean publishedOnly) {
        Page<Blog> request = Page.of(page, size);
        QueryWrapper<Blog> wrapper = new QueryWrapper<>();
        if (publishedOnly) {
            wrapper.eq("is_published", true);
        }
        wrapper.orderByDesc("created_at");
        Page<Blog> result = blogMapper.selectPage(request, wrapper);
        return new BlogPage(result.getRecords(), result.getTotal(), page, size);
    }

    public Blog findById(Long id) {
        return blogMapper.selectById(id);
    }

    public Blog createBlog(Long authorId, BlogInput input) {
        Blog blog = new Blog();
        blog.setTitle(input.title());
        blog.setSummary(input.summary());
        blog.setContent(input.content());
        blog.setCategoryId(input.categoryId());
        blog.setCoverUrl(input.coverUrl());
        blog.setPublished(Boolean.TRUE.equals(input.published()));
        blog.setAuthorId(authorId);
        blog.setViews(0L);
        blog.setLikes(0L);
        blog.setCreatedAt(LocalDateTime.now());
        blog.setUpdatedAt(LocalDateTime.now());
        blogMapper.insert(blog);

        if (input.tagIds() != null) {
            for (Long tagId : input.tagIds()) {
                BlogTag relation = new BlogTag();
                relation.setBlogId(blog.getId());
                relation.setTagId(tagId);
                blogTagMapper.insert(relation);
            }
        }
        return blog;
    }

    public Blog updateBlog(Long id, BlogInput input) {
        Blog blog = blogMapper.selectById(id);
        if (blog == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        blog.setTitle(input.title());
        blog.setSummary(input.summary());
        blog.setContent(input.content());
        blog.setCategoryId(input.categoryId());
        blog.setCoverUrl(input.coverUrl());
        blog.setPublished(Boolean.TRUE.equals(input.published()));
        blog.setUpdatedAt(LocalDateTime.now());
        blogMapper.updateById(blog);

        if (input.tagIds() != null) {
            QueryWrapper<BlogTag> wrapper = new QueryWrapper<>();
            wrapper.eq("blog_id", id);
            blogTagMapper.delete(wrapper);
            for (Long tagId : input.tagIds()) {
                BlogTag relation = new BlogTag();
                relation.setBlogId(id);
                relation.setTagId(tagId);
                blogTagMapper.insert(relation);
            }
        }
        return blog;
    }

    public boolean deleteBlog(Long id) {
        blogMapper.deleteById(id);
        QueryWrapper<BlogTag> wrapper = new QueryWrapper<>();
        wrapper.eq("blog_id", id);
        blogTagMapper.delete(wrapper);
        return true;
    }

    public List<Long> findTagIds(Long blogId) {
        return blogTagMapper.findTagIdsByBlogId(blogId);
    }
}
