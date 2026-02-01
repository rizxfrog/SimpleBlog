package com.simpleblog.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simpleblog.mapper.BlogMapper;
import com.simpleblog.mapper.BlogTagMapper;
import com.simpleblog.mapper.BlogVoteMapper;
import com.simpleblog.model.dto.BlogInput;
import com.simpleblog.model.dto.BlogPage;
import com.simpleblog.model.dto.BlogSearchPage;
import com.simpleblog.model.entity.Blog;
import com.simpleblog.model.entity.BlogTag;
import com.simpleblog.model.entity.BlogVote;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class BlogService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_HOT_LIMIT = 20;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private final BlogMapper blogMapper;
    private final BlogTagMapper blogTagMapper;
    private final BlogVoteMapper blogVoteMapper;

    public BlogService(BlogMapper blogMapper, BlogTagMapper blogTagMapper, BlogVoteMapper blogVoteMapper) {
        this.blogMapper = blogMapper;
        this.blogTagMapper = blogTagMapper;
        this.blogVoteMapper = blogVoteMapper;
    }

    public BlogPage listBlogs(int page, int size, boolean publishedOnly) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        Page<Blog> request = Page.of(safePage, safeSize);
        QueryWrapper<Blog> wrapper = new QueryWrapper<>();
        if (publishedOnly) {
            wrapper.eq("is_published", true);
        }
        wrapper.orderByDesc("created_at");
        Page<Blog> result = blogMapper.selectPage(request, wrapper);
        return new BlogPage(result.getRecords(), result.getTotal(), safePage, safeSize);
    }

    public Blog findById(Long id) {
        return blogMapper.selectById(id);
    }

    public List<Blog> listHotBlogs(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_HOT_LIMIT);
        LocalDate day = LocalDate.now(DEFAULT_ZONE);
        return blogMapper.listHotBlogs(day, safeLimit);
    }

    public BlogSearchPage searchBlogs(String query, int page, int size) {
        String normalizedQuery = query == null ? "" : query.trim();
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);

        if (normalizedQuery.isEmpty()) {
            return new BlogSearchPage(List.of(), 0, safePage, safeSize, normalizedQuery);
        }

        long offset = (long) (safePage - 1) * safeSize;
        List<Blog> items = blogMapper.searchBlogs(normalizedQuery, offset, safeSize);
        long total = blogMapper.countSearchBlogs(normalizedQuery);
        return new BlogSearchPage(items, total, safePage, safeSize, normalizedQuery);
    }

    @Transactional
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
        blog.setDislikes(0L);
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

    @Transactional
    public Blog updateBlog(Long id, BlogInput input) {
        Blog blog = blogMapper.selectById(id);
        if (blog == null) {
            throw new IllegalArgumentException("Blog not found: " + id);
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

    @Transactional
    public boolean deleteBlog(Long id) {
        blogMapper.deleteById(id);
        QueryWrapper<BlogTag> wrapper = new QueryWrapper<>();
        wrapper.eq("blog_id", id);
        blogTagMapper.delete(wrapper);
        return true;
    }

    @Transactional
    public Blog voteBlog(Long blogId, int value, Long userId, String ip) {
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("Vote value must be 1 or -1.");
        }
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null) {
            throw new IllegalArgumentException("Blog not found: " + blogId);
        }
        String voterIp = ip == null || ip.isBlank() ? "unknown" : ip;
        QueryWrapper<BlogVote> voteWrapper = new QueryWrapper<>();
        voteWrapper.eq("blog_id", blogId);
        if (userId != null) {
            voteWrapper.eq("user_id", userId);
        } else {
            voteWrapper.eq("voter_ip", voterIp);
        }
        BlogVote existing = blogVoteMapper.selectOne(voteWrapper);
        if (existing != null) {
            throw new IllegalStateException("You have already voted on this post.");
        }

        BlogVote vote = new BlogVote();
        vote.setBlogId(blogId);
        vote.setUserId(userId);
        vote.setVoterIp(userId == null ? voterIp : null);
        vote.setValue(value);
        vote.setCreatedAt(LocalDateTime.now());
        blogVoteMapper.insert(vote);

        UpdateWrapper<Blog> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", blogId);
        if (value > 0) {
            updateWrapper.setSql("likes = COALESCE(likes, 0) + 1");
        } else {
            updateWrapper.setSql("dislikes = COALESCE(dislikes, 0) + 1");
        }
        blogMapper.update(null, updateWrapper);
        return blogMapper.selectById(blogId);
    }

    public List<Long> findTagIds(Long blogId) {
        return blogTagMapper.findTagIdsByBlogId(blogId);
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size) {
        int safeSize = Math.max(size, 1);
        return Math.min(safeSize, MAX_PAGE_SIZE);
    }
}
