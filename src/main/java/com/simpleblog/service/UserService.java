package com.simpleblog.service;

import com.simpleblog.model.entity.Role;
import com.simpleblog.model.entity.User;

import java.util.List;

public interface UserService {
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象
     */
    User findByUsername(String username);

    /**
     * 根据ID查找用户
     * @param id 用户ID
     * @return 用户对象
     */
    User findById(Long id);

    /**
     * 查找用户的角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> findRoles(Long userId);
}
