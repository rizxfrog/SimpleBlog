package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.DocNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DocNodeMapper extends BaseMapper<DocNode> {
    @Select("""
            select n.*,
                   d.id as doc_id
            from doc_node n
            left join doc d on d.node_id = n.id
            where n.id = #{id}
            """)
    DocNode selectByIdWithDoc(@Param("id") Long id);

    @Select("""
            <script>
            select n.*,
                   d.id as doc_id
            from doc_node n
            left join doc d on d.node_id = n.id
            where n.space_id = #{spaceId}
            <if test="includeDeleted == false">
              and n.is_deleted = false
            </if>
            order by n.parent_id nulls first, n.sort_key asc, n.id asc
            </script>
            """)
    List<DocNode> listBySpace(@Param("spaceId") Long spaceId,
                              @Param("includeDeleted") boolean includeDeleted);

/*    @Select("""
            select coalesce(max(sort_key), -1) + 1
            from doc_node
            where space_id = #{spaceId}
              and parent_id is not distinct from #{parentId}
            """)*/
    @Select("""
            select coalesce((select sort_key + 1
            from doc_node
            where space_id = #{spaceId}
                and parent_id is not distinct from #{parentId}
            order by sort_key desc
            limit 1), 0) as next_sort_key
            """)
    Integer nextSortKey(@Param("spaceId") Long spaceId,
                        @Param("parentId") Long parentId);

    @Update("""
            with recursive subtree as (
                select id
                from doc_node
                where id = #{id}
                union all
                select n.id
                from doc_node n
                join subtree s on n.parent_id = s.id
            )
            update doc_node
            set is_deleted = true
            where id in (select id from subtree)
            """)
    int markSubtreeDeleted(@Param("id") Long id);

    @Select("""
            with recursive subtree as (
                select id
                from doc_node
                where id = #{id}
                union all
                select n.id
                from doc_node n
                join subtree s on n.parent_id = s.id
            )
            select id
            from subtree
            """)
    List<Long> listSubtreeIds(@Param("id") Long id);

    @Select("""
            with recursive subtree as (
                select id
                from doc_node
                where id = #{rootNodeId}
                union all
                select n.id
                from doc_node n
                join subtree s on n.parent_id = s.id
            )
            select count(1)
            from subtree
            where id = #{candidateId}
            """)
    long countContains(@Param("rootNodeId") Long rootNodeId,
                       @Param("candidateId") Long candidateId);

    @Update("""
            update doc_node
            set title = #{title}
            where id = #{id}
            """)
    int updateTitle(@Param("id") Long id, @Param("title") String title);
}
