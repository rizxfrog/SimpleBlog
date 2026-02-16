package com.simpleblog.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DocCommitParentMapper {
    @Insert("""
            insert into doc_commit_parent(doc_id, child_commit_id, parent_commit_id, parent_order)
            values (#{docId}, #{childCommitId}, #{parentCommitId}, #{parentOrder})
            """)
    int insertParent(@Param("docId") Long docId,
                     @Param("childCommitId") Long childCommitId,
                     @Param("parentCommitId") Long parentCommitId,
                     @Param("parentOrder") int parentOrder);
}
