package com.simpleblog.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.simpleblog.mapper.RoleMapper;
import com.simpleblog.mapper.UserMapper;
import com.simpleblog.model.entity.Role;
import com.simpleblog.model.entity.User;
import com.simpleblog.model.entity.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.enabled:true}")
    private boolean adminEnabled;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.display-name:Admin}")
    private String adminDisplayName;

    public DataInitializer(UserMapper userMapper,
                           RoleMapper roleMapper,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!adminEnabled) {
            return;
        }

        User existing = userMapper.findByUsername(adminUsername);
        if (existing != null) {
            return;
        }

        Role adminRole = roleMapper.selectOne(new QueryWrapper<Role>().eq("code", "admin"));
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setCode("admin");
            adminRole.setName("Administrator");
            roleMapper.insert(adminRole);
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setDisplayName(adminDisplayName);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setRoleId(adminRole.getId());
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(admin);
    }
}
