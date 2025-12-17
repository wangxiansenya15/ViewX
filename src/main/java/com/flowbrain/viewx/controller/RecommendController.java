package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.entity.Video;
import com.flowbrain.viewx.service.RecommendService;
import com.flowbrain.viewx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/recommend")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @Autowired
    private UserService userService;

    /**
     * Get trending videos (Hot list).
     * Accessible by everyone.
     */
    @GetMapping("/trending")
    public Result<List<com.flowbrain.viewx.pojo.vo.VideoListVO>> getTrendingVideos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching trending videos, page: {}, size: {}", page, size);
        List<com.flowbrain.viewx.pojo.vo.VideoListVO> videos = recommendService.getTrendingVideos(page, size);
        return Result.success(videos);
    }

    /**
     * Get recommended videos (Personalized feed).
     * If logged in, returns personalized content.
     * If guest, returns trending/random content.
     */
    @GetMapping("/feed")
    public Result<List<com.flowbrain.viewx.pojo.vo.VideoListVO>> getRecommendedFeed(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken)) {

            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            if (user != null) {
                userId = user.getId();
                log.info("Fetching recommended feed for user: {}", username);
            }
        } else {
            log.info("Fetching recommended feed for guest");
        }

        List<com.flowbrain.viewx.pojo.vo.VideoListVO> videos = recommendService.getRecommendedVideos(userId, page,
                size);
        return Result.success(videos);
    }

    /**
     * Initialize recommendation scores for all approved videos.
     * This endpoint should be called after deployment or when Redis is cleared.
     * Requires admin privileges (TODO: add @PreAuthorize).
     */
    @GetMapping("/init-scores")
    public Result<String> initializeScores() {
        log.info("Manually triggering recommendation score initialization");

        try {
            if (recommendService instanceof com.flowbrain.viewx.service.impl.RecommendServiceImpl) {
                ((com.flowbrain.viewx.service.impl.RecommendServiceImpl) recommendService)
                        .initializeRecommendationScores();
                return Result.success("Recommendation scores initialized successfully");
            } else {
                return Result.serverError("Service implementation does not support initialization");
            }
        } catch (Exception e) {
            log.error("Failed to initialize recommendation scores", e);
            return Result.serverError("Failed to initialize scores: " + e.getMessage());
        }
    }

    /**
     * 搜索视频（通过标题或描述）
     * GET /recommend/search?keyword=xxx&page=1&size=20
     */
    @GetMapping("/search")
    public Result<List<com.flowbrain.viewx.pojo.vo.VideoListVO>> searchVideos(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        log.info("搜索视频，关键词: {}, 页码: {}, 大小: {}", keyword, page, size);

        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.badRequest("搜索关键词不能为空");
        }

        Long userId = null;
        if (authentication != null) {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            if (user != null) {
                userId = user.getId();
            }
        }

        return recommendService.searchVideos(keyword.trim(), userId, page, size);
    }
}
