package com.simpleblog.model.dto;

import java.time.LocalDateTime;

public record BlogVoteEvent(
        Long blogId,
        Long userId,
        String voterIp,
        int newValue,
        LocalDateTime occurredAt
) {
}
