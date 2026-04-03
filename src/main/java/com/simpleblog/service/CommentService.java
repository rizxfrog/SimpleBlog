package com.simpleblog.service;

import com.simpleblog.model.dto.CommentInput;
import com.simpleblog.model.entity.Comment;
import com.simpleblog.model.enums.CommentStatus;

import java.util.List;

public interface CommentService {
    /**
     * 查询博客的已审核评论列表
     * @param blogId 博客ID
     * @return 评论列表
     */
    List<Comment> listApprovedByBlogId(Long blogId);

    /**
     * 查询博客的评论列表(指定状态)
     * @param blogId 博客ID
     * @param status 评论状态
     * @return 评论列表
     */
    List<Comment> listByBlogId(Long blogId, CommentStatus status);

    /**
     * 查询博客的评论列表(支持状态和关键词过滤)
     * @param blogId 博客ID
     * @param status 评论状态
     * @param keyword 搜索关键词
     * @return 评论列表
     */
    List<Comment> listByBlogId(Long blogId, CommentStatus status, String keyword);

    /**
     * 创建评论
     * @param userId 用户ID(可为空表示匿名用户)
     * @param input 评论输入
     * @param authorIp 作者IP
     * @param authorUa 作者User-Agent
     * @return 创建的评论
     */
    Comment create(Long userId, CommentInput input, String authorIp, String authorUa);

    /**
     * 创建回复评论
     * @param userId 用户ID
     * @param commentId 父评论ID
     * @param input 评论输入
     * @param authorIp 作者IP
     * @param authorUa 作者User-Agent
     * @return 创建的评论
     */
    Comment createReply(Long userId, Long commentId, CommentInput input, String authorIp, String authorUa);

    /**
     * 更新评论状态
     * @param commentId 评论ID
     * @param status 新状态
     * @return 更新后的评论
     */
    Comment updateStatus(Long commentId, CommentStatus status);

    /**
     * 投票评论
     * @param commentId 评论ID
     * @param value 投票值(1或-1)
     * @param userId 用户ID
     * @param voterIp 投票者IP
     * @return 更新后的评论
     */
    Comment vote(Long commentId, int value, Long userId, String voterIp);

    /**
     * 删除评论
     * @param commentId 评论ID
     * @return 是否成功
     */
    boolean delete(Long commentId);

    /**
     * 批量更新评论状态
     * @param ids 评论ID列表
     * @param status 新状态
     * @return 是否成功
     */
    boolean batchUpdateStatus(List<Long> ids, CommentStatus status);
}
