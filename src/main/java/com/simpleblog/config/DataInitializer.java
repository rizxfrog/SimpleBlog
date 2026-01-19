package com.simpleblog.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.simpleblog.mapper.RoleMapper;
import com.simpleblog.mapper.UserMapper;
import com.simpleblog.model.entity.Role;
import com.simpleblog.model.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final JdbcTemplate jdbcTemplate;
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
                           JdbcTemplate jdbcTemplate,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.jdbcTemplate = jdbcTemplate;
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
        admin.setStatus("active");
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(admin);

        Integer existingRelation = jdbcTemplate.queryForObject(
                "select count(1) from user_roles where user_id = ? and role_id = ?",
                Integer.class,
                admin.getId(),
                adminRole.getId()
        );
        if (existingRelation == null || existingRelation == 0) {
            jdbcTemplate.update(
                    "insert into user_roles(user_id, role_id) values (?, ?)",
                    admin.getId(),
                    adminRole.getId()
            );
        }
    }
}
