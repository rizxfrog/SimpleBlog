package com.simpleblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.simpleblog.config.RabbitConfig;
import com.simpleblog.mapper.BlogMapper;
import com.simpleblog.mapper.BlogVoteMapper;
import com.simpleblog.model.dto.BlogVoteEvent;
import com.simpleblog.model.entity.Blog;
import com.simpleblog.model.entity.BlogVote;
import com.simpleblog.service.BlogVoteEventListener;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlogVoteEventListenerImpl implements BlogVoteEventListener {
    private final BlogVoteMapper blogVoteMapper;
    private final BlogMapper blogMapper;

    public BlogVoteEventListenerImpl(BlogVoteMapper blogVoteMapper, BlogMapper blogMapper) {
        this.blogVoteMapper = blogVoteMapper;
        this.blogMapper = blogMapper;
    }

    @Transactional
    @RabbitListener(queues = RabbitConfig.BLOG_VOTE_QUEUE)
    @Override
    public void handle(BlogVoteEvent event) {
        if (event == null || event.blogId() == null) {
            return;
        }
        Integer newValue = event.newValue();
        if (newValue != 1 && newValue != -1 && newValue != 0) {
            return;
        }

        QueryWrapper<BlogVote> wrapper = new QueryWrapper<>();
        wrapper.eq("blog_id", event.blogId());
        if (event.userId() != null) {
            wrapper.eq("user_id", event.userId());
        } else {
            wrapper.eq("voter_ip", normalizeIp(event.voterIp()));
        }
        BlogVote existing = blogVoteMapper.selectOne(wrapper);
        Integer existingValue = existing == null ? null : existing.getValue();

        if (existingValue == null && newValue == 0) {
            return;
        }
        if (existingValue != null && newValue != 0 && existingValue.equals(newValue)) {
            return;
        }

        if (existingValue == null && newValue != 0) {
            BlogVote vote = new BlogVote();
            vote.setBlogId(event.blogId());
            vote.setUserId(event.userId());
            vote.setVoterIp(event.userId() == null ? normalizeIp(event.voterIp()) : null);
            vote.setValue(newValue);
            vote.setCreatedAt(event.occurredAt());
            blogVoteMapper.insert(vote);
            applyDelta(event.blogId(), newValue, 0);
            return;
        }

        if (existingValue != null && newValue == 0) {
            blogVoteMapper.deleteById(existing.getId());
            applyDelta(event.blogId(), 0, existingValue);
            return;
        }

        if (existing != null && newValue != 0 && !newValue.equals(existingValue)) {
            existing.setValue(newValue);
            blogVoteMapper.updateById(existing);
            applyDelta(event.blogId(), newValue, existingValue == null ? 0 : existingValue);
        }
    }

    private void applyDelta(Long blogId, int newValue, int oldValue) {
        UpdateWrapper<Blog> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", blogId);
        if (newValue == oldValue) {
            return;
        }
        if (newValue == 1 && oldValue == 0) {
            wrapper.setSql("likes = COALESCE(likes, 0) + 1");
        } else if (newValue == -1 && oldValue == 0) {
            wrapper.setSql("dislikes = COALESCE(dislikes, 0) + 1");
        } else if (newValue == 0 && oldValue == 1) {
            wrapper.setSql("likes = GREATEST(COALESCE(likes, 0) - 1, 0)");
        } else if (newValue == 0 && oldValue == -1) {
            wrapper.setSql("dislikes = GREATEST(COALESCE(dislikes, 0) - 1, 0)");
        } else if (newValue == 1 && oldValue == -1) {
            wrapper.setSql("""
                likes = COALESCE(likes, 0) + 1,
                dislikes = GREATEST(COALESCE(dislikes, 0) - 1, 0)
                """);
        } else if (newValue == -1 && oldValue == 1) {
            wrapper.setSql("""
                dislikes = COALESCE(dislikes, 0) + 1,
                likes = GREATEST(COALESCE(likes, 0) - 1, 0)
                """);
        }
        blogMapper.update(null, wrapper);
    }

    private String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip;
    }
}
