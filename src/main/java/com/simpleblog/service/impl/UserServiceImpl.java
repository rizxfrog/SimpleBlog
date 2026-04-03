package com.simpleblog.service.impl;

import com.simpleblog.mapper.RoleMapper;
import com.simpleblog.mapper.UserMapper;
import com.simpleblog.model.entity.Role;
import com.simpleblog.model.entity.User;
import com.simpleblog.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public UserServiceImpl(UserMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public List<Role> findRoles(Long userId) {
        return roleMapper.findRolesByUserId(userId);
    }
}
