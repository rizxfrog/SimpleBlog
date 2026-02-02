package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.Blog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface BlogMapper extends BaseMapper<Blog> {
    @Select("""
        with query_args as (
          select
            #{query}::text as q,
            websearch_to_tsquery('simple', #{query}) as tsq
        ),
        scored as (
          select
            b.*,
            ts_rank_cd(b.search_vector, qa.tsq) as rank,
            replace(
              replace(
                ts_headline(
                  'simple',
                  b.title,
                  qa.tsq,
                  'StartSel=[[[, StopSel=]]], MaxFragments=1, MaxWords=20, MinWords=5'
                ),
                '[[[',
                '&lt;mark&gt;'
              ),
              ']]]',
              '&lt;/mark&gt;'
            ) as title_highlight,
            replace(
              replace(
                ts_headline(
                  'simple',
                  coalesce(b.summary, ''),
                  qa.tsq,
                  'StartSel=[[[, StopSel=]]], MaxFragments=1, MaxWords=36, MinWords=10'
                ),
                '[[[',
                '&lt;mark&gt;'
              ),
              ']]]',
              '&lt;/mark&gt;'
            ) as summary_highlight
          from blogs b
          cross join query_args qa
          where b.is_published = true
            and (
              b.search_vector @@ qa.tsq
              or similarity(
                coalesce(b.title, '') || ' ' || coalesce(b.summary, '') || ' ' || coalesce(b.content, ''),
                qa.q
              ) > 0.08
              or (coalesce(b.title, '') || ' ' || coalesce(b.summary, '') || ' ' || coalesce(b.content, ''))
                ilike '%' || qa.q || '%'
            )
        )
        select *
        from scored
        order by rank desc nulls last, created_at desc
        offset #{offset}
        limit #{size}
        """)
    List<Blog> searchBlogs(@Param("query") String query, @Param("offset") long offset, @Param("size") int size);

    @Select("""
        with query_args as (
          select
            #{query}::text as q,
            websearch_to_tsquery('simple', #{query}) as tsq
        )
        select count(1)
        from blogs b
        cross join query_args qa
        where b.is_published = true
          and (
            b.search_vector @@ qa.tsq
            or similarity(
              coalesce(b.title, '') || ' ' || coalesce(b.summary, '') || ' ' || coalesce(b.content, ''),
              qa.q
            ) > 0.08
            or (coalesce(b.title, '') || ' ' || coalesce(b.summary, '') || ' ' || coalesce(b.content, ''))
              ilike '%' || qa.q || '%'
          )
        """)
    long countSearchBlogs(@Param("query") String query);

    @Select("""
        select
          b.*,
          (
            (coalesce(b.likes, 0) - coalesce(b.dislikes, 0)) * 20
            + coalesce(pv.views, 0) * 9
            + coalesce(b.views, 0)
          ) as rank
        from blogs b
        left join article_pv_daily pv
          on pv.blog_id = b.id
         and pv.day = #{day}
        where b.is_published = true
        order by rank desc nulls last, b.created_at desc
        limit #{limit}
        """)
    List<Blog> listHotBlogs(@Param("day") LocalDate day, @Param("limit") int limit);
}
