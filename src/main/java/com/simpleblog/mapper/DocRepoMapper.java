package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.DocRepo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DocRepoMapper extends BaseMapper<DocRepo> {
    @Select("""
            select *
            from doc
            where node_id = #{nodeId}
            """)
    DocRepo selectByNodeId(@Param("nodeId") Long nodeId);

    @Select("""
            select count(1)
            from doc
            where id = #{id}
            """)
    long countById(@Param("id") Long id);

    @Select("""
            select node_id
            from doc
            where id = #{docId}
            """)
    Long findNodeIdByDocId(@Param("docId") Long docId);
}
