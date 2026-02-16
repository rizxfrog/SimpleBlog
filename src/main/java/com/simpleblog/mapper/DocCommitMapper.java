package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.DocCommit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocCommitMapper extends BaseMapper<DocCommit> {
    @Select("""
            insert into doc_commit(doc_id, commit_hash, author_id, message, title, content_md)
            values (#{docId}, #{commitHash}, #{authorId}, #{message}, #{title}, #{contentMd})
            returning id
            """)
    Long insertReturningId(@Param("docId") Long docId,
                           @Param("commitHash") byte[] commitHash,
                           @Param("authorId") Long authorId,
                           @Param("message") String message,
                           @Param("title") String title,
                           @Param("contentMd") String contentMd);

    @Select("""
            select id,
                   doc_id,
                   encode(commit_hash, 'hex') as commit_hash,
                   author_id,
                   message,
                   create_at,
                   title,
                   content_md,
                   encode(content_hash, 'hex') as content_hash
            from doc_commit
            where doc_id = #{docId}
              and id = #{id}
            """)
    DocCommit selectByDocAndId(@Param("docId") Long docId, @Param("id") Long id);

    @Select("""
            select count(1)
            from doc_commit
            where doc_id = #{docId}
              and id = #{commitId}
            """)
    long countByDocAndId(@Param("docId") Long docId, @Param("commitId") Long commitId);

    @Select("""
            select c.id,
                   c.doc_id,
                   encode(c.commit_hash, 'hex') as commit_hash,
                   c.author_id,
                   c.message,
                   c.create_at,
                   c.title,
                   c.content_md,
                   encode(c.content_hash, 'hex') as content_hash
            from doc_ref r
            join doc_commit c
              on c.doc_id = r.doc_id
             and c.id = r.commit_id
            where r.doc_id = #{docId}
              and r.ref_name = #{refName}
            """)
    DocCommit findLatestByRef(@Param("docId") Long docId, @Param("refName") String refName);

    @Select("""
            with recursive chain as (
                select c.id, c.create_at, 0 as depth
                from doc_ref r
                join doc_commit c
                  on c.doc_id = r.doc_id
                 and c.id = r.commit_id
                where r.doc_id = #{docId}
                  and r.ref_name = #{refName}
                union all
                select p.parent_commit_id, c2.create_at, ch.depth + 1
                from chain ch
                join doc_commit_parent p
                  on p.doc_id = #{docId}
                 and p.child_commit_id = ch.id
                 and p.parent_order = 0
                join doc_commit c2
                  on c2.doc_id = p.doc_id
                 and c2.id = p.parent_commit_id
                where ch.depth < #{maxDepth}
            )
            select c.id,
                   c.doc_id,
                   encode(c.commit_hash, 'hex') as commit_hash,
                   c.author_id,
                   c.message,
                   c.create_at,
                   c.title,
                   c.content_md,
                   encode(c.content_hash, 'hex') as content_hash,
                   ch.depth
            from chain ch
            join doc_commit c
              on c.doc_id = #{docId}
             and c.id = ch.id
            order by ch.depth asc
            """)
    List<DocCommit> listHistory(@Param("docId") Long docId,
                                @Param("refName") String refName,
                                @Param("maxDepth") int maxDepth);
}
