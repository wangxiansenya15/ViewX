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
}
