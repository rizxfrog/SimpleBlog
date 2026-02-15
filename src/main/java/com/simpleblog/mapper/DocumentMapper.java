package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.Document;
import org.apache.ibatis.annotations.Delete;
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
          doc_project = #{project}
          and doc_version = #{version}
          <if test='includeHidden == false'>
            and is_hidden = false
          </if>
        </where>
        order by parent_id nulls first, sort_order asc, id asc
        </script>
        """)
    List<Document> listAll(@Param("includeHidden") boolean includeHidden,
                           @Param("project") String project,
                           @Param("version") String version);

    @Select("""
        <script>
        select *
        from documents
        where doc_project = #{project}
          and doc_version = #{version}
          and path &lt;@ #{rootPath}::ltree
        <if test='includeHidden == false'>
          and is_hidden = false
        </if>
        order by parent_id nulls first, sort_order asc, id asc
        </script>
        """)
    List<Document> listSubtree(@Param("rootPath") String rootPath,
                               @Param("includeHidden") boolean includeHidden,
                               @Param("project") String project,
                               @Param("version") String version);

    @Select("""
        select *
        from documents
        where doc_project = #{project}
          and doc_version = #{version}
        order by parent_id nulls first, sort_order asc, id asc
        """)
    List<Document> listAllByProjectVersion(@Param("project") String project,
                                           @Param("version") String version);

    @Select("""
        select *
        from documents
        where doc_project = #{project}
        order by doc_version asc, parent_id nulls first, sort_order asc, id asc
        """)
    List<Document> listAllByProject(@Param("project") String project);

    @Select("""
        select path::text
        from documents
        where id = #{id}
          and doc_project = #{project}
          and doc_version = #{version}
        """)
    String findPathByIdAndScope(@Param("id") Long id,
                                @Param("project") String project,
                                @Param("version") String version);

    @Select("""
        select count(1)
        from documents
        where path = #{path}::ltree
          and doc_project = #{project}
          and doc_version = #{version}
        """)
    long countByPath(@Param("path") String path,
                     @Param("project") String project,
                     @Param("version") String version);

    @Select("""
        select count(1)
        from documents
        where path = #{path}::ltree
          and id &lt;&gt; #{id}
          and doc_project = #{project}
          and doc_version = #{version}
        """)
    long countByPathExcludingId(@Param("path") String path,
                                @Param("id") Long id,
                                @Param("project") String project,
                                @Param("version") String version);

    @Select("""
        select count(1)
        from documents
        where id = #{candidateId}
          and path &lt;@ #{ancestorPath}::ltree
          and doc_project = #{project}
          and doc_version = #{version}
        """)
    long countDescendantOf(@Param("candidateId") Long candidateId,
                           @Param("ancestorPath") String ancestorPath,
                           @Param("project") String project,
                           @Param("version") String version);

    @Update("""
        update documents
        set path = #{newPath}::ltree || subpath(path, nlevel(#{oldPath}::ltree))
        where path &lt;@ #{oldPath}::ltree
          and doc_project = #{project}
          and doc_version = #{version}
        """)
    int updatePathPrefix(@Param("oldPath") String oldPath,
                         @Param("newPath") String newPath,
                         @Param("project") String project,
                         @Param("version") String version);

    @Select("""
        select distinct doc_project
        from documents
        order by doc_project asc
        """)
    List<String> listProjects();

    @Select("""
        select distinct doc_version
        from documents
        where doc_project = #{project}
        order by doc_version asc
        """)
    List<String> listVersions(@Param("project") String project);

    @Select("""
        select count(1)
        from documents
        where doc_project = #{project}
          and doc_version = #{version}
        """)
    long countByProjectVersion(@Param("project") String project,
                               @Param("version") String version);

    @Select("""
        select count(1)
        from documents
        where doc_project = #{project}
        """)
    long countByProject(@Param("project") String project);

    @Delete("""
        delete from documents
        where doc_project = #{project}
          and doc_version = #{version}
        """)
    int deleteByProjectVersion(@Param("project") String project,
                               @Param("version") String version);

    @Delete("""
        delete from documents
        where doc_project = #{project}
        """)
    int deleteByProject(@Param("project") String project);
}
