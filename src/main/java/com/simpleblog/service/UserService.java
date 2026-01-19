package com.simpleblog.service;

import com.simpleblog.mapper.RoleMapper;
import com.simpleblog.mapper.UserMapper;
import com.simpleblog.model.entity.Role;
import com.simpleblog.model.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public UserService(UserMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    public List<Role> findRoles(Long userId) {
        return roleMapper.findRolesByUserId(userId);
    }
}
