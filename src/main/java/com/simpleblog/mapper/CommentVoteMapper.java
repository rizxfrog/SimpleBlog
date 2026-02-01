package com.simpleblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simpleblog.model.entity.CommentVote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentVoteMapper extends BaseMapper<CommentVote> {
}
