package com.simpleblog.service;

import com.simpleblog.mapper.UserMapper;
import com.simpleblog.model.dto.AuthPayload;
import com.simpleblog.model.entity.Role;
import com.simpleblog.model.entity.User;
import com.simpleblog.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthPayload login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        List<Role> roles = userService.findRoles(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roleCodes);

        String token = jwtService.generateToken(user.getUsername(), claims);
        return new AuthPayload(token, user);
    }
}
