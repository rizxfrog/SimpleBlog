package com.simpleblog.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simpleblog.mapper.BlogMapper;
import com.simpleblog.mapper.BlogTagMapper;
import com.simpleblog.mapper.BlogVoteMapper;
import com.simpleblog.model.dto.BlogInput;
import com.simpleblog.model.dto.BlogVoteEvent;
import com.simpleblog.model.dto.BlogPage;
import com.simpleblog.model.dto.BlogSearchPage;
import com.simpleblog.model.entity.Blog;
import com.simpleblog.model.entity.BlogTag;
import com.simpleblog.model.entity.BlogVote;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.List;

@Service
public class BlogService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_HOT_LIMIT = 20;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private final BlogMapper blogMapper;
    private final BlogTagMapper blogTagMapper;
    private final BlogVoteMapper blogVoteMapper;
    private final BlogSearchService blogSearchService;
    private final StringRedisTemplate redisTemplate;
    private final BlogVoteEventPublisher blogVoteEventPublisher;

    public BlogService(BlogMapper blogMapper,
                       BlogTagMapper blogTagMapper,
                       BlogVoteMapper blogVoteMapper,
                       BlogSearchService blogSearchService,
                       StringRedisTemplate redisTemplate,
                       BlogVoteEventPublisher blogVoteEventPublisher) {
        this.blogMapper = blogMapper;
        this.blogTagMapper = blogTagMapper;
        this.blogVoteMapper = blogVoteMapper;
        this.blogSearchService = blogSearchService;
        this.redisTemplate = redisTemplate;
        this.blogVoteEventPublisher = blogVoteEventPublisher;
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

    public BlogSearchPage searchBlogsEs(String query, int page, int size) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        BlogSearchService.SearchResult result = blogSearchService.search(query, safePage, safeSize);
        return new BlogSearchPage(result.items(), result.total(), safePage, safeSize, query == null ? "" : query.trim());
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
        blogSearchService.indexBlog(blog);

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
        blogSearchService.indexBlog(blog);

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
        blogSearchService.deleteBlog(id);
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
        String voterIp = normalizeIp(ip);
        String voterKey = voterKey(blogId, userId, voterIp);
        Integer current = parseInt(redisTemplate.opsForValue().get(voterKey));
        int newValue = value;
        if (current != null && current == value) {
            newValue = 0;
        }

        ensureVoteCountsInitialized(blog);
        applyVoteToRedis(blogId, current == null ? 0 : current, newValue);
        if (newValue == 0) {
            redisTemplate.delete(voterKey);
        } else {
            redisTemplate.opsForValue().set(voterKey, String.valueOf(newValue), Duration.ofDays(7));
        }

        BlogVoteEvent event = new BlogVoteEvent(
                blogId,
                userId,
                voterIp,
                newValue,
                LocalDateTime.now()
        );
        blogVoteEventPublisher.publish(event);
        return blogMapper.selectById(blogId);
    }

    public Integer getUserVote(Long blogId, Long userId, String ip) {
        if (blogId == null) {
            return null;
        }
        String voterIp = normalizeIp(ip);
        String voterKey = voterKey(blogId, userId, voterIp);
        Integer cached = parseInt(redisTemplate.opsForValue().get(voterKey));
        if (cached != null) {
            return cached;
        }
        QueryWrapper<BlogVote> wrapper = new QueryWrapper<>();
        wrapper.eq("blog_id", blogId);
        if (userId != null) {
            wrapper.eq("user_id", userId);
        } else {
            wrapper.eq("voter_ip", voterIp);
        }
        BlogVote vote = blogVoteMapper.selectOne(wrapper);
        if (vote == null || vote.getValue() == null) {
            return null;
        }
        redisTemplate.opsForValue().set(voterKey, String.valueOf(vote.getValue()), Duration.ofDays(7));
        return vote.getValue();
    }

    public long getLikes(Long blogId, long fallback) {
        return getVoteCount(blogId, "likes", fallback);
    }

    public long getDislikes(Long blogId, long fallback) {
        return getVoteCount(blogId, "dislikes", fallback);
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

    private void applyVoteToRedis(Long blogId, int oldValue, int newValue) {
        if (oldValue == newValue) {
            return;
        }
        String key = voteCountKey(blogId);
        if (newValue == 1 && oldValue == 0) {
            redisTemplate.opsForHash().increment(key, "likes", 1);
        } else if (newValue == -1 && oldValue == 0) {
            redisTemplate.opsForHash().increment(key, "dislikes", 1);
        } else if (newValue == 0 && oldValue == 1) {
            redisTemplate.opsForHash().increment(key, "likes", -1);
        } else if (newValue == 0 && oldValue == -1) {
            redisTemplate.opsForHash().increment(key, "dislikes", -1);
        } else if (newValue == 1 && oldValue == -1) {
            redisTemplate.opsForHash().increment(key, "likes", 1);
            redisTemplate.opsForHash().increment(key, "dislikes", -1);
        } else if (newValue == -1 && oldValue == 1) {
            redisTemplate.opsForHash().increment(key, "dislikes", 1);
            redisTemplate.opsForHash().increment(key, "likes", -1);
        }
    }

    private long getVoteCount(Long blogId, String field, long fallback) {
        String value = (String) redisTemplate.opsForHash().get(voteCountKey(blogId), field);
        if (value != null) {
            return parseLong(value);
        }
        redisTemplate.opsForHash().put(voteCountKey(blogId), field, String.valueOf(fallback));
        return fallback;
    }

    private void ensureVoteCountsInitialized(Blog blog) {
        if (blog == null || blog.getId() == null) {
            return;
        }
        String key = voteCountKey(blog.getId());
        Object likes = redisTemplate.opsForHash().get(key, "likes");
        Object dislikes = redisTemplate.opsForHash().get(key, "dislikes");
        if (likes == null) {
            redisTemplate.opsForHash().put(key, "likes", String.valueOf(blog.getLikes()));
        }
        if (dislikes == null) {
            redisTemplate.opsForHash().put(key, "dislikes", String.valueOf(blog.getDislikes()));
        }
    }

    private String voterKey(Long blogId, Long userId, String voterIp) {
        if (userId != null) {
            return "vote:blog:" + blogId + ":user:" + userId;
        }
        return "vote:blog:" + blogId + ":ip:" + voterIp;
    }

    private String voteCountKey(Long blogId) {
        return "vote:blog:" + blogId + ":counts";
    }

    private Integer parseInt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private long parseLong(String value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip;
    }
}
