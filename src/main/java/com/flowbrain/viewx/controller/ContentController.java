package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.ContentUploadDTO;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.vo.ContentDetailVO;
import com.flowbrain.viewx.pojo.vo.ContentVO;
import com.flowbrain.viewx.service.ContentService;
import com.flowbrain.viewx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 内容控制器 - 支持图片、图片集等多种内容类型
 */
@RestController
@RequestMapping("/contents")
@Slf4j
public class ContentController {

    @Autowired
    private ContentService contentService;

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
     * 上传单张图片
     * POST /contents/image
     */
    @PostMapping("/image")
    public Result<Long> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subcategory", required = false) String subcategory,
            @RequestParam(value = "tags", required = false) String[] tags,
            @RequestParam(value = "visibility", required = false, defaultValue = "PUBLIC") String visibility) {
        
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        ContentUploadDTO dto = new ContentUploadDTO();
        dto.setContentType("IMAGE");
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setCategory(category);
        dto.setSubcategory(subcategory);
        dto.setTags(tags);
        dto.setVisibility(visibility);

        return contentService.uploadImage(userId, file, dto);
    }

    /**
     * 上传图片集
     * POST /contents/image-set
     */
    @PostMapping("/image-set")
    public Result<Long> uploadImageSet(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subcategory", required = false) String subcategory,
            @RequestParam(value = "tags", required = false) String[] tags,
            @RequestParam(value = "visibility", required = false, defaultValue = "PUBLIC") String visibility) {
        
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        ContentUploadDTO dto = new ContentUploadDTO();
        dto.setContentType("IMAGE_SET");
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setCategory(category);
        dto.setSubcategory(subcategory);
        dto.setTags(tags);
        dto.setVisibility(visibility);

        return contentService.uploadImageSet(userId, Arrays.asList(files), dto);
    }

    /**
     * 获取内容详情
     * GET /contents/{id}
     */
    @GetMapping("/{id}")
    public Result<ContentDetailVO> getContentDetail(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return contentService.getContentDetail(id, userId);
    }

    /**
     * 获取用户的内容列表
     * GET /contents/user/{userId}?type=IMAGE
     */
    @GetMapping("/user/{userId}")
    public Result<List<ContentVO>> getUserContents(
            @PathVariable Long userId,
            @RequestParam(value = "type", required = false) String contentType) {
        return contentService.getUserContents(userId, contentType);
    }

    /**
     * 获取我的内容列表
     * GET /contents/my?type=IMAGE
     */
    @GetMapping("/my")
    public Result<List<ContentVO>> getMyContents(
            @RequestParam(value = "type", required = false) String contentType) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return contentService.getUserContents(userId, contentType);
    }

    /**
     * 删除内容
     * DELETE /contents/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteContent(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return contentService.deleteContent(userId, id);
    }
}
