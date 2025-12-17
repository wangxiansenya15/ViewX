package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.UserProfileDTO;
import com.flowbrain.viewx.pojo.vo.UserProfileVO;
import com.flowbrain.viewx.service.ProfileService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户资料控制器
 * 展示 Controller 层如何使用 DTO 和 VO
 */
@RestController
@RequestMapping("/user/profile")
@Slf4j
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    /**
     * 获取当前登录用户的资料
     * 返回类型：Result<UserProfileVO>
     */
    @GetMapping("/me")
    public Result<UserProfileVO> getMyProfile(Authentication authentication) {
        String username = authentication.getName();
        log.info("用户 {} 查看个人资料", username);

        // 从 authentication 中获取用户ID（实际项目中需要实现）
        Long userId = getUserIdFromAuth(authentication);

        return profileService.getUserProfile(userId);
    }

    /**
     * 获取指定用户的资料（公开信息）
     * 
     * @param userId 用户ID
     * @return 用户资料 VO
     */
    @GetMapping("/{userId}")
    public Result<UserProfileVO> getUserProfile(@PathVariable Long userId) {
        log.info("查看用户资料，用户ID: {}", userId);
        return profileService.getUserProfile(userId);
    }

    /**
     * 更新当前用户的资料
     * 接收类型：UserProfileDTO（带验证）
     * 返回类型：Result<UserProfileVO>
     * 
     * @param dto 用户资料更新 DTO
     * @return 更新后的用户资料 VO
     */
    @PutMapping("/me")
    public Result<UserProfileVO> updateMyProfile(
            @Valid @RequestBody UserProfileDTO dto,
            Authentication authentication) {

        String username = authentication.getName();
        Long userId = getUserIdFromAuth(authentication);

        // 获取当前用户的角色
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority())
                .orElse("ROLE_USER");

        log.info("用户 {} (角色: {}) 更新个人资料", username, role);
        return profileService.updateUserProfile(userId, dto, userId, role);
    }

    @Autowired
    private com.flowbrain.viewx.service.UserService userService;

    /**
     * 上传头像
     * 
     * @param file           头像文件
     * @param authentication 认证信息
     * @return 头像URL
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
                                  Authentication authentication) {

        String username = authentication.getName();
        Long userId = getUserIdFromAuth(authentication);

        log.info("用户 {} 上传头像，文件大小: {} bytes", username, file.getSize());

        try {
            // 1. 验证文件
            validateAvatarFile(file);

            // 2. 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = "avatar_" + userId + "_" + System.currentTimeMillis() + extension;

            // 3. 存储文件
            String avatarUrl = profileService.uploadAvatar(userId, file, filename);

            log.info("用户 {} 头像上传成功: {}", username, avatarUrl);
            return Result.success("头像上传成功", avatarUrl);

        } catch (IllegalArgumentException e) {
            log.warn("头像上传失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.serverError("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 验证头像文件
     */
    private void validateAvatarFile(org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 验证文件大小（最大 5MB）
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小不能超过 5MB");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("只支持图片格式");
        }

        // 验证文件扩展名
        String filename = file.getOriginalFilename();
        if (filename == null || !isValidImageExtension(filename)) {
            throw new IllegalArgumentException("只支持 jpg, jpeg, png, gif, webp 格式");
        }
    }

    /**
     * 验证图片扩展名
     */
    private boolean isValidImageExtension(String filename) {
        String[] validExtensions = { ".jpg", ".jpeg", ".png", ".gif", ".webp" };
        String lowerFilename = filename.toLowerCase();
        for (String ext : validExtensions) {
            if (lowerFilename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // 默认扩展名
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    /**
     * 从 Authentication 中提取用户ID
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("未认证的用户");
        }
        String username = authentication.getName();
        com.flowbrain.viewx.pojo.entity.User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + username);
        }
        return user.getId();
    }
}
