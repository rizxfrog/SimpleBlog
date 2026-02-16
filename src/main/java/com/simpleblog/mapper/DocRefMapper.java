package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.DocRef;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DocRefMapper extends BaseMapper<DocRef> {
    @Select("""
            select *
            from doc_ref
            where doc_id = #{docId}
            order by ref_type asc, ref_name asc
            """)
    List<DocRef> listByDocId(@Param("docId") Long docId);

    @Select("""
            select *
            from doc_ref
            where doc_id = #{docId}
              and ref_name = #{refName}
            """)
    DocRef selectByDocAndName(@Param("docId") Long docId, @Param("refName") String refName);

    @Select("""
            select commit_id
            from doc_ref
            where doc_id = #{docId}
              and ref_name = #{refName}
            """)
    Long findRefTip(@Param("docId") Long docId, @Param("refName") String refName);

    @Select("""
            select commit_id
            from doc_ref
            where doc_id = #{docId}
              and ref_name = #{refName}
            for update
            """)
    Long lockRefTip(@Param("docId") Long docId, @Param("refName") String refName);

    @Insert("""
            insert into doc_ref(doc_id, ref_name, commit_id, ref_type)
            values (#{docId}, #{refName}, #{commitId}, cast(#{refType} as doc_ref_type))
            """)
    int insertRef(@Param("docId") Long docId,
                  @Param("refName") String refName,
                  @Param("commitId") Long commitId,
                  @Param("refType") String refType);

    @Update("""
            update doc_ref
            set commit_id = #{commitId}
            where doc_id = #{docId}
              and ref_name = #{refName}
            """)
    int updateRefCommit(@Param("docId") Long docId,
                        @Param("refName") String refName,
                        @Param("commitId") Long commitId);

    @Delete("""
            delete from doc_ref
            where doc_id = #{docId}
              and ref_name = #{refName}
            """)
    int deleteRef(@Param("docId") Long docId, @Param("refName") String refName);
}
