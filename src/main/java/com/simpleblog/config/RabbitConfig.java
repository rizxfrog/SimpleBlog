package com.simpleblog.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String BLOG_EVENT_EXCHANGE = "simpleblog.events";
    public static final String BLOG_VOTE_QUEUE = "simpleblog.blog.vote";
    public static final String BLOG_VOTE_ROUTING_KEY = "blog.vote";

    @Bean
    public DirectExchange blogEventExchange() {
        return new DirectExchange(BLOG_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue blogVoteQueue() {
        return new Queue(BLOG_VOTE_QUEUE, true);
    }

    @Bean
    public Binding blogVoteBinding(DirectExchange blogEventExchange, Queue blogVoteQueue) {
        return BindingBuilder.bind(blogVoteQueue).to(blogEventExchange).with(BLOG_VOTE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
