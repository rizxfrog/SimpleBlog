package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
    @Select("""
        <script>
        select *
        from documents
        <where>
          <if test='includeHidden == false'>
            is_hidden = false
          </if>
        </where>
        order by parent_id nulls first, sort_order asc, id asc
        </script>
        """)
    List<Document> listAll(@Param("includeHidden") boolean includeHidden);

    @Select("""
        <script>
        select *
        from documents
        where path &lt;@ #{rootPath}::ltree
        <if test='includeHidden == false'>
          and is_hidden = false
        </if>
        order by parent_id nulls first, sort_order asc, id asc
        </script>
        """)
    List<Document> listSubtree(@Param("rootPath") String rootPath,
                               @Param("includeHidden") boolean includeHidden);

    @Select("""
        select path::text
        from documents
        where id = #{id}
        """)
    String findPathById(@Param("id") Long id);

    @Select("""
        select count(1)
        from documents
        where path = #{path}::ltree
        """)
    long countByPath(@Param("path") String path);

    @Select("""
        select count(1)
        from documents
        where path = #{path}::ltree
          and id <> #{id}
        """)
    long countByPathExcludingId(@Param("path") String path,
                                @Param("id") Long id);

    @Select("""
        select count(1)
        from documents
        where id = #{candidateId}
          and path <@ #{ancestorPath}::ltree
        """)
    long countDescendantOf(@Param("candidateId") Long candidateId,
                           @Param("ancestorPath") String ancestorPath);

    @Update("""
        update documents
        set path = #{newPath}::ltree || subpath(path, nlevel(#{oldPath}::ltree))
        where path &lt;@ #{oldPath}::ltree
        """)
    int updatePathPrefix(@Param("oldPath") String oldPath,
                         @Param("newPath") String newPath);
}
