package com.simpleblog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simpleblog.common.utils.RedisKeyBuilder;
import com.simpleblog.common.utils.RedisUtils;
import com.simpleblog.mapper.UserMapper;
import com.simpleblog.model.entity.User;
import com.simpleblog.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
public class JwtService {
    private static final long CURRENT_USER_ID_CACHE_TTL_MINUTES = 180;
    private final SecretKey secretKey;
    private final long expirationSeconds;
    private final RedisUtils redisUtils;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds,
                      RedisUtils redisUtils,
                      UserMapper userMapper,
                      ObjectMapper objectMapper
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationSeconds = expirationSeconds;
        this.redisUtils = redisUtils;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    public String generateToken(String username, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long currentUserId() {
        return SecurityUtils.currentUserId().orElseThrow(() -> new IllegalStateException("User not authenticated."));
    }

    public User currentUser() {
        String username = SecurityUtils.currentUsername()
                .orElseThrow(() -> new IllegalStateException("User not authenticated."));
        String key = RedisKeyBuilder.userByUsername(username);

        User cachedUser = parseUser(redisUtils.get(key));
        if (cachedUser != null && cachedUser.getId() != null) {
            return cachedUser;
        }

        User user = userMapper.findByUsername(username);
        if (user == null || user.getId() == null) {
            throw new IllegalStateException("User not found: " + username);
        }

        writeUserCache(key, user);
        return user;
    }

    private User parseUser(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, User.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private void writeUserCache(String key, User user) {
        try {
            redisUtils.setEx(
                    key,
                    objectMapper.writeValueAsString(user),
                    CURRENT_USER_ID_CACHE_TTL_MINUTES + ThreadLocalRandom.current().nextInt(0, 30),
                    TimeUnit.MINUTES
            );
        } catch (Exception ignored) {
        }
    }
}
