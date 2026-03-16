package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.DocSpace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocSpaceMapper extends BaseMapper<DocSpace> {
    @Select("""
            select *
            from doc_space
            order by name asc, id asc
            """)
    List<DocSpace> listAll();

    @Select("""
            select count(1)
            from doc_space
            where lower(name) = lower(#{name})
              and (#{excludingId} is null or id <> #{excludingId})
            """)
    long countByName(@Param("name") String name, @Param("excludingId") Long excludingId);

    @Select("""
        select 1
        from doc_space
        where id = #{id}
            and not is_deleted
        limit 1
        """)
    boolean isExistByIdNotDeleted(@Param("id") Long id);
}
