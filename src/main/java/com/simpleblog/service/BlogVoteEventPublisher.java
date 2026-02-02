package com.simpleblog.service;

import com.simpleblog.config.RabbitConfig;
import com.simpleblog.model.dto.BlogVoteEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class BlogVoteEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public BlogVoteEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(BlogVoteEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.BLOG_EVENT_EXCHANGE, RabbitConfig.BLOG_VOTE_ROUTING_KEY, event);
    }
}
