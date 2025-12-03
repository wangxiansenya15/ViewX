package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.service.InteractionService;
import com.flowbrain.viewx.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/interactions")
public class InteractionController {

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            User user = userService.getUserByUsername(username);
            return user != null ? user.getId() : null;
        }
        return null;
    }

    @PostMapping("/like/{videoId}")
    public Result<?> likeVideo(@PathVariable Long videoId) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        return interactionService.toggleLike(userId, videoId);
    }

    @PostMapping("/favorite/{videoId}")
    public Result<?> favoriteVideo(@PathVariable Long videoId) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
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
}
