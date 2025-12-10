package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.CommentCreateDTO;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.vo.CommentVO;
import com.flowbrain.viewx.service.InteractionService;
import com.flowbrain.viewx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/interactions")
@Slf4j
public class InteractionController {

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            User user = userService.getUserByUsername(username);
            return user != null ? user.getId() : null;
        }
        return null;
    }

    // ==================== 点赞相关 ====================

    @PostMapping("/like/{videoId}")
    public Result<?> likeVideo(@PathVariable Long videoId) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return interactionService.toggleLike(userId, videoId);
    }

    // ==================== 收藏相关 ====================

    @PostMapping("/favorite/{videoId}")
    public Result<?> favoriteVideo(@PathVariable Long videoId) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return interactionService.toggleFavorite(userId, videoId);
    }

    @GetMapping("/status/{videoId}")
    public Result<?> getInteractionStatus(@PathVariable Long videoId) {
        Long userId = getCurrentUserId();
        Map<String, Boolean> status = new HashMap<>();
        if (userId == null) {
            status.put("liked", false);
            status.put("favorited", false);
        } else {
            status.put("liked", interactionService.isLiked(userId, videoId));
            status.put("favorited", interactionService.isFavorited(userId, videoId));
        }
        return Result.success(status);
    }

    // ==================== 评论相关 ====================

    /**
     * 发表评论
     */
    @PostMapping("/comments")
    public Result<CommentVO> createComment(@RequestBody CommentCreateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return interactionService.createComment(userId, dto);
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comments/{commentId}")
    public Result<String> deleteComment(@PathVariable Long commentId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return interactionService.deleteComment(userId, commentId);
    }

    /**
     * 获取视频评论列表
     */
    @GetMapping("/comments/{videoId}")
    public Result<List<CommentVO>> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        return interactionService.getVideoComments(videoId, userId, page, size);
    }

    /**
     * 点赞评论
     */
    @PostMapping("/comments/{commentId}/like")
    public Result<String> toggleCommentLike(@PathVariable Long commentId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return interactionService.toggleCommentLike(userId, commentId);
    }

    // ==================== 关注相关 ====================

    /**
     * 关注/取消关注用户
     */
    @PostMapping("/follow/{userId}")
    public Result<String> toggleFollow(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "请先登录");
        }
        return interactionService.toggleFollow(currentUserId, userId);
    }

    /**
     * 检查是否关注某用户
     */
    @GetMapping("/follow/status/{userId}")
    public Result<Boolean> isFollowing(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return Result.success(false);
        }
        boolean following = interactionService.isFollowing(currentUserId, userId);
        return Result.success(following);
    }

    /**
     * 获取用户的粉丝数和关注数
     */
    @GetMapping("/follow/stats/{userId}")
    public Result<Map<String, Long>> getFollowStats(@PathVariable Long userId) {
        long followerCount = interactionService.getFollowerCount(userId);
        long followingCount = interactionService.getFollowingCount(userId);
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("followerCount", followerCount);
        stats.put("followingCount", followingCount);
        
        return Result.success(stats);
    }
}
