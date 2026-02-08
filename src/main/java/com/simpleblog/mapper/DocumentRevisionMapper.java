package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.DocumentRevision;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentRevisionMapper extends BaseMapper<DocumentRevision> {
    @Select("""
        select *
        from document_revisions
        where document_id = #{documentId}
        order by revision_number desc, created_at desc
        """)
    List<DocumentRevision> listByDocumentId(@Param("documentId") Long documentId);

    @Select("""
        select coalesce(max(revision_number), 0)
        from document_revisions
        where document_id = #{documentId}
        """)
    int maxRevisionNumber(@Param("documentId") Long documentId);
}
