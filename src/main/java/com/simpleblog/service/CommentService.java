package com.simpleblog.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.simpleblog.mapper.CommentMapper;
import com.simpleblog.model.dto.CommentInput;
import com.simpleblog.model.entity.Comment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {
    private final CommentMapper commentMapper;

    public CommentService(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    public List<Comment> listByBlogId(Long blogId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("blog_id", blogId).orderByAsc("created_at");
        return commentMapper.selectList(wrapper);
    }

    public Comment create(Long userId, CommentInput input) {
        Comment comment = new Comment();
        comment.setBlogId(input.blogId());
        comment.setParentId(input.parentId());
        comment.setUserId(userId);
        comment.setContent(input.content());
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        return comment;
    }
}
