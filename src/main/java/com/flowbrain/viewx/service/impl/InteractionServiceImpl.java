package com.flowbrain.viewx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.dao.*;
import com.flowbrain.viewx.pojo.dto.CommentCreateDTO;
import com.flowbrain.viewx.pojo.entity.VideoComment;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.entity.UserDetail;
import com.flowbrain.viewx.pojo.entity.UserFollow;
import com.flowbrain.viewx.pojo.vo.CommentVO;
import com.flowbrain.viewx.pojo.vo.UserSummaryVO;
import com.flowbrain.viewx.service.EventPublisher;
import com.flowbrain.viewx.service.InteractionService;
import com.flowbrain.viewx.service.RecommendService;
import com.flowbrain.viewx.service.StorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户交互服务实现类
 */
@Service
@Slf4j
public class InteractionServiceImpl implements InteractionService {

    @Autowired
    private InteractionMapper interactionMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserDetailMapper userDetailMapper;

    @Autowired
    private RecommendService recommendService;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private StorageStrategy storageStrategy;

    // ==================== 点赞相关 ====================

    @Override
    @Transactional
    public Result<String> toggleLike(Long userId, Long videoId) {
        if (interactionMapper.checkLike(userId, videoId) > 0) {
            interactionMapper.deleteLike(userId, videoId);
            interactionMapper.decrementVideoLikeCount(videoId);
            eventPublisher.publishLikeEvent(userId, videoId, false);
            return Result.success("取消点赞");
        } else {
            interactionMapper.insertLike(userId, videoId);
            interactionMapper.incrementVideoLikeCount(videoId);
            eventPublisher.publishLikeEvent(userId, videoId, true);
            return Result.success("点赞成功");
        }
    }

    @Override
    public boolean isLiked(Long userId, Long videoId) {
        return interactionMapper.checkLike(userId, videoId) > 0;
    }

    // ==================== 收藏相关 ====================

    @Override
    @Transactional
    public Result<String> toggleFavorite(Long userId, Long videoId) {
        if (interactionMapper.checkFavorite(userId, videoId) > 0) {
            interactionMapper.deleteFavorite(userId, videoId);
            return Result.success("取消收藏");
        } else {
            interactionMapper.insertFavorite(userId, videoId);
            eventPublisher.publishFavoriteEvent(userId, videoId);
            recommendService.updateVideoScore(videoId);
            return Result.success("收藏成功");
        }
    }

    @Override
    public boolean isFavorited(Long userId, Long videoId) {
        return interactionMapper.checkFavorite(userId, videoId) > 0;
    }

    // ==================== 评论相关 ====================

    @Override
    @Transactional
    public Result<CommentVO> createComment(Long userId, CommentCreateDTO dto) {
        try {
            // 创建评论实体
            VideoComment comment = new VideoComment();
            comment.setVideoId(dto.getVideoId());
            comment.setUserId(userId);
            comment.setParentId(dto.getParentId());
            comment.setContent(dto.getContent());
            comment.setLikeCount(0);
            comment.setIsPinned(false);
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());

            // 插入评论
            commentMapper.insert(comment);

            // 更新视频评论数
            commentMapper.incrementVideoCommentCount(dto.getVideoId());

            // 发布评论事件（异步通知、统计等）
            eventPublisher.publishCommentEvent(userId, dto.getVideoId(), comment.getId(), dto.getContent());

            // 转换为 VO 返回
            CommentVO vo = convertToCommentVO(comment, userId);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("发表评论失败", e);
            return Result.serverError("发表评论失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<String> deleteComment(Long userId, Long commentId) {
        // 查询评论
        VideoComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return Result.notFound("评论不存在");
        }

        // 权限检查：只能删除自己的评论
        if (!comment.getUserId().equals(userId)) {
            return Result.forbidden("无权删除此评论");
        }

        // 软删除评论
        commentMapper.deleteById(commentId);
        return Result.success("删除成功");
    }

    @Override
    public Result<List<CommentVO>> getVideoComments(Long videoId, Long currentUserId, int page, int size) {
        try {
            // 查询一级评论
            QueryWrapper<VideoComment> query = new QueryWrapper<>();
            query.eq("video_id", videoId)
                    .isNull("parent_id")
                    .orderByDesc("is_pinned")
                    .orderByDesc("created_at")
                    .last("LIMIT " + size + " OFFSET " + ((page - 1) * size));

            List<VideoComment> comments = commentMapper.selectList(query);

            // 转换为 VO 并填充回复
            List<CommentVO> vos = comments.stream()
                    .map(comment -> {
                        CommentVO vo = convertToCommentVO(comment, currentUserId);
                        // 加载回复
                        vo.setReplies(loadReplies(comment.getId(), currentUserId));
                        return vo;
                    })
                    .collect(Collectors.toList());

            return Result.success(vos);
        } catch (Exception e) {
            log.error("获取评论列表失败", e);
            return Result.serverError("获取评论列表失败");
        }
    }

    @Override
    @Transactional
    public Result<String> toggleCommentLike(Long userId, Long commentId) {
        // TODO: 实现评论点赞功能（需要创建评论点赞表）
        return Result.error(501, "功能开发中");
    }

    // ==================== 关注相关 ====================

    @Override
    @Transactional
    public Result<String> toggleFollow(Long followerId, Long followedId) {
        // 防止自己关注自己
        if (followerId.equals(followedId)) {
            return Result.badRequest("不能关注自己");
        }

        if (followMapper.checkFollow(followerId, followedId) > 0) {
            followMapper.deleteFollow(followerId, followedId);
            eventPublisher.publishFollowEvent(followerId, followedId, false);
            return Result.success("取消关注");
        } else {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(followerId);
            follow.setFollowedId(followedId);
            follow.setCreatedAt(LocalDateTime.now());
            followMapper.insertFollow(follow);
            eventPublisher.publishFollowEvent(followerId, followedId, true);
            return Result.success("关注成功");
        }
    }

    @Override
    public boolean isFollowing(Long followerId, Long followedId) {
        return followMapper.checkFollow(followerId, followedId) > 0;
    }

    @Override
    public boolean isMutualFollow(Long userId1, Long userId2) {
        // 检查双向关注
        return followMapper.checkFollow(userId1, userId2) > 0
                && followMapper.checkFollow(userId2, userId1) > 0;
    }

    @Override
    public long getFollowerCount(Long userId) {
        return followMapper.getFollowerCount(userId);
    }

    @Override
    public long getFollowingCount(Long userId) {
        return followMapper.getFollowingCount(userId);
    }

    @Override
    public Result<List<UserSummaryVO>> getFollowers(Long userId, Long currentUserId, int page, int size) {
        int offset = (page - 1) * size;
        List<UserSummaryVO> list = followMapper.getFollowers(userId, offset, size);

        // 批量获取当前用户关注的所有用户ID（避免N+1查询）
        java.util.Set<Long> followingIds = new java.util.HashSet<>();
        if (currentUserId != null) {
            followingIds.addAll(followMapper.getFollowingUserIds(currentUserId));
        }

        // Process avatars and follow status
        for (UserSummaryVO user : list) {
            if (user.getAvatar() != null && !user.getAvatar().startsWith("http")) {
                user.setAvatar(storageStrategy.getFileUrl(user.getAvatar()));
            }
            // 使用 Set 快速判断，O(1) 时间复杂度
            user.setIsFollowing(followingIds.contains(user.getId()));
        }
        return Result.success(list);
    }

    @Override
    public Result<List<UserSummaryVO>> getFollowing(Long userId, Long currentUserId, int page, int size) {
        int offset = (page - 1) * size;
        List<UserSummaryVO> list = followMapper.getFollowing(userId, offset, size);

        // 批量获取当前用户关注的所有用户ID（避免N+1查询）
        java.util.Set<Long> followingIds = new java.util.HashSet<>();
        if (currentUserId != null) {
            followingIds.addAll(followMapper.getFollowingUserIds(currentUserId));
        }

        // Process avatars and follow status
        for (UserSummaryVO user : list) {
            if (user.getAvatar() != null && !user.getAvatar().startsWith("http")) {
                user.setAvatar(storageStrategy.getFileUrl(user.getAvatar()));
            }
            // 使用 Set 快速判断，O(1) 时间复杂度
            user.setIsFollowing(followingIds.contains(user.getId()));
        }
        return Result.success(list);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 转换评论实体为 VO
     */
    private CommentVO convertToCommentVO(VideoComment comment, Long currentUserId) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);

        // 填充用户信息
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());

            // 获取头像
            QueryWrapper<UserDetail> detailQuery = new QueryWrapper<>();
            detailQuery.eq("user_id", comment.getUserId());
            UserDetail detail = userDetailMapper.selectOne(detailQuery);
            if (detail != null && detail.getAvatar() != null) {
                if (!detail.getAvatar().startsWith("http")) {
                    vo.setUserAvatar(storageStrategy.getFileUrl(detail.getAvatar()));
                } else {
                    vo.setUserAvatar(detail.getAvatar());
                }
            }
        }

        // 检查当前用户是否点赞了该评论
        // TODO: 实现评论点赞检查
        vo.setIsLiked(false);

        return vo;
    }

    /**
     * 加载评论的回复列表
     */
    private List<CommentVO> loadReplies(Long parentId, Long currentUserId) {
        QueryWrapper<VideoComment> query = new QueryWrapper<>();
        query.eq("parent_id", parentId)
                .orderByAsc("created_at");

        List<VideoComment> replies = commentMapper.selectList(query);
        return replies.stream()
                .map(reply -> convertToCommentVO(reply, currentUserId))
                .collect(Collectors.toList());
    }
}
