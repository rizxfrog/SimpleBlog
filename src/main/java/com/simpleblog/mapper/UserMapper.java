package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.User;
import com.simpleblog.mapper.typehandler.UserStatusTypeHandler;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("select * from users where username = #{username} limit 1")
    @Results(id = "userResultMap", value = {
            @Result(column = "status", property = "status", typeHandler = UserStatusTypeHandler.class)
    })
    User findByUsername(String username);
}
