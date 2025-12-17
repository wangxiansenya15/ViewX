package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.VideoUploadDTO;
import com.flowbrain.viewx.pojo.dto.VideoUpdateDTO;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.vo.VideoDetailVO;
import com.flowbrain.viewx.service.UserService;
import com.flowbrain.viewx.service.VideoService;
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
     * Upload video with metadata
     * 上传视频（包含文件和元数据）
     * 支持同时上传封面图片
     */
    @PostMapping
    public Result<Long> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestParam("title") String title,
            @RequestParam("duration") Integer duration,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subcategory", required = false) String subcategory,
            @RequestParam(value = "coverUrl", required = false) String coverUrl,
            @RequestParam(value = "thumbnailUrl", required = false) String thumbnailUrl,
            @RequestParam(value = "tags", required = false) String[] tags,
            @RequestParam(value = "visibility", required = false, defaultValue = "PUBLIC") String visibility) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        // 构建DTO
        VideoUploadDTO dto = new VideoUploadDTO();
        dto.setTitle(title);
        dto.setDuration(duration);
        dto.setDescription(description);
        dto.setCategory(category);
        dto.setSubcategory(subcategory);
        dto.setCoverUrl(coverUrl);
        dto.setThumbnailUrl(thumbnailUrl);
        dto.setTags(tags);
        dto.setVisibility(visibility);

        return videoService.uploadVideo(userId, file, coverFile, dto);
    }

    /**
     * Update video
     */
    @PutMapping("/{id}")
    public Result<String> updateVideo(@PathVariable Long id, @RequestBody VideoUpdateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return videoService.updateVideo(userId, id, dto);
    }

    /**
     * Upload cover image
     * 单独上传封面图片（返回封面URL和缩略图URL）
     */
    @PostMapping("/upload/cover")
    public Result<com.flowbrain.viewx.pojo.vo.CoverUploadVO> uploadCover(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return videoService.uploadCoverImage(file);
    }

    /**
     * Delete video
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteVideo(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return videoService.deleteVideo(userId, id);
    }

    /**
     * Get my videos
     */
    @GetMapping("/my")
    public Result<java.util.List<com.flowbrain.viewx.pojo.entity.Video>> getMyVideos() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return videoService.getMyVideos(userId);
    }

    /**
     * Get user's videos by userId
     * 获取指定用户的视频列表（公开）
     */
    @GetMapping("/user/{userId}")
    public Result<java.util.List<com.flowbrain.viewx.pojo.entity.Video>> getUserVideos(@PathVariable Long userId) {
        log.info("获取用户视频列表，用户ID: {}", userId);
        return videoService.getMyVideos(userId);
    }
}
