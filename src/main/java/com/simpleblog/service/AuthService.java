package com.simpleblog.service;

import com.simpleblog.model.dto.AuthPayload;
import com.simpleblog.model.entity.User;

public interface AuthService {
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 认证载荷(包含token和用户信息)
     */
    AuthPayload login(String username, String password);

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @param displayName 显示名称
     * @param email 邮箱
     * @return 认证载荷(包含token和用户信息)
     */
    AuthPayload register(String username, String password, String displayName, String email);
}
