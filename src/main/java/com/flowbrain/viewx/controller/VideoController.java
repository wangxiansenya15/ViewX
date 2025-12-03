package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.VideoCreateDTO;
import com.flowbrain.viewx.pojo.dto.VideoUpdateDTO;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.vo.VideoDetailVO;
import com.flowbrain.viewx.service.UserService;
import com.flowbrain.viewx.service.VideoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/videos")
@Slf4j
public class VideoController {

    @Autowired
    private VideoService videoService;

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

    /**
     * Get video details
     */
    @GetMapping("/{id}")
    public Result<VideoDetailVO> getVideoDetail(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return videoService.getVideoDetail(id, userId);
    }

    /**
     * Create/Publish a new video
     */
    @PostMapping
    public Result<Long> createVideo(@Valid @RequestBody VideoCreateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return videoService.createVideo(userId, dto);
    }

    /**
     * Update video
     */
    @PutMapping("/{id}")
    public Result<String> updateVideo(@PathVariable Long id, @RequestBody VideoUpdateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return videoService.updateVideo(userId, id, dto);
    }

    /**
     * Upload video file
     */
    @PostMapping("/upload")
    public Result<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return videoService.uploadVideoFile(file);
    }

    /**
     * Upload cover image
     */
    @PostMapping("/upload/cover")
    public Result<String> uploadCover(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return videoService.uploadCoverImage(file);
    }
}
