package com.simpleblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.simpleblog.mapper.RoleMapper;
import com.simpleblog.mapper.UserMapper;
import com.simpleblog.model.dto.AuthPayload;
import com.simpleblog.model.entity.Role;
import com.simpleblog.model.entity.User;
import com.simpleblog.model.entity.UserStatus;
import com.simpleblog.security.JwtService;
import com.simpleblog.service.AuthService;
import com.simpleblog.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthPayload login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("User is not active.");
        }

        List<Role> roles = userService.findRoles(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getCode).toList();

        Map<String, Object> claims = Map.of(
                "username", user.getUsername(),
                "roles", roleCodes,
                "user_id", user.getId()
        );

        String token = jwtService.generateToken(user.getUsername(), claims);
        return new AuthPayload(token, user);
    }

    @Override
    public AuthPayload register(String username, String password, String displayName, String email) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required.");
        }
        if (userMapper.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists.");
        }

        Role userRole = roleMapper.selectOne(new QueryWrapper<Role>().eq("code", "user"));
        if (userRole == null) {
            userRole = new Role();
            userRole.setCode("user");
            userRole.setName("User");
            roleMapper.insert(userRole);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName((displayName == null || displayName.isBlank()) ? username : displayName);
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoleId(userRole.getId());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);    // 自增类型插入后会自动回填id

        List<Role> roles = userService.findRoles(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getCode).toList();

        Map<String, Object> claims = Map.of(
                "username", user.getUsername(),
                "roles", roleCodes,
                "user_id", user.getId()
        );

        String token = jwtService.generateToken(user.getUsername(), claims);
        return new AuthPayload(token, user);
    }
}
