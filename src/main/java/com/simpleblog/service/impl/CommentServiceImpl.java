package com.simpleblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.simpleblog.mapper.CommentMapper;
import com.simpleblog.mapper.CommentVoteMapper;
import com.simpleblog.model.dto.CommentInput;
import com.simpleblog.model.entity.Comment;
import com.simpleblog.model.entity.CommentVote;
import com.simpleblog.model.enums.CommentStatus;
import com.simpleblog.service.CommentRateLimiter;
import com.simpleblog.service.CommentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CommentServiceImpl implements CommentService {
    private static final int ANONYMOUS_LIMIT = 3;
    private static final long ANONYMOUS_WINDOW_MS = 5 * 60 * 1000L;

    private final CommentMapper commentMapper;
    private final CommentVoteMapper commentVoteMapper;
    private final CommentRateLimiter rateLimiter;

    public CommentServiceImpl(CommentMapper commentMapper, CommentVoteMapper commentVoteMapper, CommentRateLimiter rateLimiter) {
        this.commentMapper = commentMapper;
        this.commentVoteMapper = commentVoteMapper;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public List<Comment> listApprovedByBlogId(Long blogId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("blog_id", blogId)
                .eq("status", CommentStatus.approved.name())
                .orderByAsc("created_at");
        return commentMapper.selectList(wrapper);
    }

    @Override
    public List<Comment> listByBlogId(Long blogId, CommentStatus status) {
        return listByBlogId(blogId, status, null);
    }

    @Override
    public List<Comment> listByBlogId(Long blogId, CommentStatus status, String keyword) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        if (blogId != null) {
            wrapper.eq("blog_id", blogId);
        }
        if (status != null) {
            wrapper.eq("status", status.name());
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("content", keyword)
                    .or()
                    .like("author_name", keyword));
        }
        wrapper.orderByAsc("created_at");
        return commentMapper.selectList(wrapper);
    }

    @Override
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

    @Override
    public Comment createReply(Long userId, Long commentId, CommentInput input, String authorIp, String authorUa) {
        Comment parent = commentMapper.selectById(commentId);
        if (parent == null) {
            throw new IllegalArgumentException("Comment not found.");
        }
        Comment reply = create(userId, new CommentInput(
                parent.getBlogId(),
                parent.getId(),
                input.content(),
                input.authorName(),
                input.authorEmail(),
                input.authorWebsite()
        ), authorIp, authorUa);
        return reply;
    }

    @Override
    public Comment updateStatus(Long commentId, CommentStatus status) {
        Comment existing = commentMapper.selectById(commentId);
        if (existing == null) {
            throw new IllegalArgumentException("Comment not found.");
        }
        existing.setStatus(status.name());
        commentMapper.updateById(existing);
        return existing;
    }

    @Override
    public Comment vote(Long commentId, int value, Long userId, String voterIp) {
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("Vote value must be 1 or -1.");
        }
        if (userId == null && (voterIp == null || voterIp.isBlank())) {
            throw new IllegalArgumentException("Voter identity is required.");
        }
        // prevent duplicate votes by user or IP
        if (userId != null && existsVote(commentId, userId, null, value)) {
            return commentMapper.selectById(commentId);
        }
        if (userId == null && existsVote(commentId, null, voterIp, value)) {
            return commentMapper.selectById(commentId);
        }

        CommentVote vote = new CommentVote();
        vote.setCommentId(commentId);
        vote.setUserId(userId);
        vote.setVoterIp(voterIp);
        vote.setValue(value);
        vote.setCreatedAt(LocalDateTime.now());
        commentVoteMapper.insert(vote);

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

    @Override
    public boolean delete(Long commentId) {
        return commentMapper.deleteById(commentId) > 0;
    }

    @Override
    public boolean batchUpdateStatus(List<Long> ids, CommentStatus status) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        UpdateWrapper<Comment> wrapper = new UpdateWrapper<>();
        wrapper.in("id", ids).set("status", status.name());
        return commentMapper.update(null, wrapper) > 0;
    }

    private boolean existsVote(Long commentId, Long userId, String voterIp, int value) {
        QueryWrapper<CommentVote> wrapper = new QueryWrapper<>();
        wrapper.eq("comment_id", commentId).eq("value", value);
        if (userId != null) {
            wrapper.eq("user_id", userId);
        } else if (voterIp != null) {
            wrapper.eq("voter_ip", voterIp);
        }
        return commentVoteMapper.selectCount(wrapper) > 0;
    }
}
