package com.simpleblog.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.simpleblog.mapper.CommentMapper;
import com.simpleblog.model.dto.CommentInput;
import com.simpleblog.model.entity.Comment;
import com.simpleblog.model.enums.CommentStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CommentService {
    private static final int ANONYMOUS_LIMIT = 3;
    private static final long ANONYMOUS_WINDOW_MS = 5 * 60 * 1000L;

    private final CommentMapper commentMapper;
    private final CommentRateLimiter rateLimiter;

    public CommentService(CommentMapper commentMapper, CommentRateLimiter rateLimiter) {
        this.commentMapper = commentMapper;
        this.rateLimiter = rateLimiter;
    }

    public List<Comment> listApprovedByBlogId(Long blogId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("blog_id", blogId)
                .eq("status", CommentStatus.approved.name())
                .orderByAsc("created_at");
        return commentMapper.selectList(wrapper);
    }

    public List<Comment> listByBlogId(Long blogId, CommentStatus status) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        if (blogId != null) {
            wrapper.eq("blog_id", blogId);
        }
        wrapper.orderByAsc("created_at");
        if (status != null) {
            wrapper.eq("status", status.name());
        }
        return commentMapper.selectList(wrapper);
    }

    public Comment create(Long userId, CommentInput input, String authorIp, String authorUa) {
        if (userId == null && (input.authorName() == null || input.authorName().isBlank())) {
            throw new IllegalArgumentException("authorName is required for anonymous comments.");
        }

        if (userId == null) {
            if (!rateLimiter.allow(authorIp, ANONYMOUS_LIMIT, ANONYMOUS_WINDOW_MS)) {
                throw new IllegalStateException("Anonymous comment rate limit exceeded.");
            }
        }

        Comment comment = new Comment();
        comment.setBlogId(input.blogId());
        comment.setParentId(input.parentId());
        comment.setUserId(userId);
        comment.setContent(input.content());
        comment.setAuthorName(input.authorName());
        comment.setAuthorEmail(input.authorEmail());
        comment.setAuthorWebsite(input.authorWebsite());
        comment.setAuthorIp(authorIp);
        comment.setAuthorUa(authorUa);
        comment.setStatus(CommentStatus.pending.name());
        comment.setUpvotes(0L);
        comment.setDownvotes(0L);
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        return comment;
    }

    public Comment updateStatus(Long commentId, CommentStatus status) {
        Comment existing = commentMapper.selectById(commentId);
        if (existing == null) {
            throw new IllegalArgumentException("Comment not found.");
        }
        existing.setStatus(status.name());
        commentMapper.updateById(existing);
        return existing;
    }

    public Comment vote(Long commentId, int value) {
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("Vote value must be 1 or -1.");
        }
        UpdateWrapper<Comment> wrapper = new UpdateWrapper<>();
        if (value == 1) {
            wrapper.setSql("upvotes = COALESCE(upvotes, 0) + 1");
        } else {
            wrapper.setSql("downvotes = COALESCE(downvotes, 0) + 1");
        }
        wrapper.eq("id", commentId);
        commentMapper.update(null, wrapper);
        return Optional.ofNullable(commentMapper.selectById(commentId))
                .orElseThrow(() -> new IllegalArgumentException("Comment not found."));
    }
}
