package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    @Select("select r.* from roles r join user_roles ur on r.id = ur.role_id where ur.user_id = #{userId}")
    List<Role> findRolesByUserId(Long userId);
}
