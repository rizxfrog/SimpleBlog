package com.simpleblog.service.impl;

import com.simpleblog.config.RabbitConfig;
import com.simpleblog.model.dto.BlogVoteEvent;
import com.simpleblog.service.BlogVoteEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class BlogVoteEventPublisherImpl implements BlogVoteEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public BlogVoteEventPublisherImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(BlogVoteEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.BLOG_EVENT_EXCHANGE, RabbitConfig.BLOG_VOTE_ROUTING_KEY, event);
    }
}
