package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.BlogVote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogVoteMapper extends BaseMapper<BlogVote> {
}
