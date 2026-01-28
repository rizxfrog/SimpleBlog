package com.simpleblog.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.simpleblog.mapper.RoleMapper;
import com.simpleblog.mapper.UserMapper;
import com.simpleblog.model.dto.AuthPayload;
import com.simpleblog.model.entity.Role;
import com.simpleblog.model.entity.User;
import com.simpleblog.model.entity.UserStatus;
import com.simpleblog.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper,
                       RoleMapper roleMapper,
                       UserService userService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthPayload login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("User is not active.");
        }

        List<Role> roles = userService.findRoles(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roleCodes);

        String token = jwtService.generateToken(user.getUsername(), claims);
        return new AuthPayload(token, user);
    }

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
        userMapper.insert(user);

        List<Role> roles = userService.findRoles(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roleCodes);

        String token = jwtService.generateToken(user.getUsername(), claims);
        return new AuthPayload(token, user);
    }
}


