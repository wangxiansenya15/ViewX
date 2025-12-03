package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.entity.VideoComment;
import com.flowbrain.viewx.service.CommentService;
import com.flowbrain.viewx.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

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

    @PostMapping("/{videoId}")
    public Result<?> addComment(@PathVariable Long videoId, @RequestBody Map<String, Object> payload) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        
        String content = (String) payload.get("content");
        Long parentId = payload.containsKey("parentId") ? ((Number) payload.get("parentId")).longValue() : null;
        
        return commentService.addComment(userId, videoId, content, parentId);
    }

    @GetMapping("/{videoId}")
    public Result<List<VideoComment>> getComments(@PathVariable Long videoId) {
        return commentService.getComments(videoId);
    }

    @GetMapping("/replies/{parentId}")
    public Result<List<VideoComment>> getReplies(@PathVariable Long parentId) {
        return commentService.getReplies(parentId);
    }
}
