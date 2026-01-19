package com.simpleblog.model.dto;

import com.simpleblog.model.entity.User;

public record AuthPayload(String token, User user) {
}
