package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.ArticlePvDaily;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticlePvDailyMapper extends BaseMapper<ArticlePvDaily> {
    @Insert("""
        insert into article_pv_daily (blog_id, day, views)
        values (#{blogId}, #{day}, #{views})
        on conflict (blog_id, day)
        do update set views = excluded.views
        """)
    void upsert(ArticlePvDaily pv);
}
