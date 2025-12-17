package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.CommentCreateDTO;
import com.flowbrain.viewx.pojo.vo.CommentVO;
import com.flowbrain.viewx.pojo.vo.UserSummaryVO;

import java.util.List;

/**
 * 用户交互服务接口
 * 包括点赞、收藏、评论、关注等功能
 */
public interface InteractionService {

    // ==================== 点赞相关 ====================

    /**
     * 切换点赞状态（点赞/取消点赞）
     */
    Result<String> toggleLike(Long userId, Long videoId);

    /**
     * 检查是否已点赞
     */
    boolean isLiked(Long userId, Long videoId);

    // ==================== 收藏相关 ====================

    /**
     * 切换收藏状态（收藏/取消收藏）
     */
    Result<String> toggleFavorite(Long userId, Long videoId);

    /**
     * 检查是否已收藏
     */
    boolean isFavorited(Long userId, Long videoId);

    // ==================== 评论相关 ====================

    /**
     * 发表评论
     */
    Result<CommentVO> createComment(Long userId, CommentCreateDTO dto);

    /**
     * 删除评论
     */
    Result<String> deleteComment(Long userId, Long commentId);

    /**
     * 获取视频的评论列表（分页）
     */
    Result<List<CommentVO>> getVideoComments(Long videoId, Long currentUserId, int page, int size);

    /**
     * 点赞评论
     */
    Result<String> toggleCommentLike(Long userId, Long commentId);

    // ==================== 关注相关 ====================

    /**
     * 关注/取消关注用户
     */
    Result<String> toggleFollow(Long followerId, Long followedId);

    /**
     * 检查是否关注某用户
     */
    boolean isFollowing(Long followerId, Long followedId);

    /**
     * 检查是否相互关注
     */
    boolean isMutualFollow(Long userId1, Long userId2);

    /**
     * 获取粉丝数
     */
    long getFollowerCount(Long userId);

    /**
     * 获取关注数
     */
    long getFollowingCount(Long userId);

    /**
     * 获取粉丝列表
     */
    Result<List<UserSummaryVO>> getFollowers(Long userId, Long currentUserId, int page, int size);

    /**
     * 获取关注列表
     */
    Result<List<UserSummaryVO>> getFollowing(Long userId, Long currentUserId, int page, int size);
}
