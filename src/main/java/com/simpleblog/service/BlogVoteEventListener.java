package com.simpleblog.service;

import com.simpleblog.model.dto.BlogVoteEvent;

public interface BlogVoteEventListener {
    /**
     * 处理博客投票事件
     * @param event 投票事件
     */
    void handle(BlogVoteEvent event);
}
