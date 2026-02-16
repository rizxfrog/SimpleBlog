package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.DocHead;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DocHeadMapper extends BaseMapper<DocHead> {
    @Select("""
            select count(1)
            from doc_head
            where doc_id = #{docId}
            """)
    long countByDocId(@Param("docId") Long docId);

    @Select("""
            select head_ref
            from doc_head
            where doc_id = #{docId}
            """)
    String findHeadRef(@Param("docId") Long docId);

    @Insert("""
            insert into doc_head(doc_id, head_ref, ref_version)
            values (#{docId}, #{headRef}, 0)
            """)
    int insertHead(@Param("docId") Long docId, @Param("headRef") String headRef);
}
