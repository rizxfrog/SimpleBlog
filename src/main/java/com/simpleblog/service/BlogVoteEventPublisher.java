package com.simpleblog.service;

import com.simpleblog.model.dto.BlogVoteEvent;

public interface BlogVoteEventPublisher {
    /**
     * 发布博客投票事件
     * @param event 投票事件
     */
    void publish(BlogVoteEvent event);
}
