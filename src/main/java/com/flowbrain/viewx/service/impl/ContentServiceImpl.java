package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.dao.ContentMapper;
import com.flowbrain.viewx.dao.UserMapper;
import com.flowbrain.viewx.pojo.dto.ContentUploadDTO;
import com.flowbrain.viewx.pojo.entity.Content;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.vo.ContentDetailVO;
import com.flowbrain.viewx.pojo.vo.ContentVO;
import com.flowbrain.viewx.service.ContentService;
import com.flowbrain.viewx.service.InteractionService;
import com.flowbrain.viewx.service.TopicService;
import com.flowbrain.viewx.service.VideoProcessingService;
import com.flowbrain.viewx.util.FilePathUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContentServiceImpl implements ContentService {

    @Autowired
    private ContentMapper contentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LocalStorageStrategy storageStrategy;

    @Autowired
    private VideoProcessingService videoProcessingService;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private com.flowbrain.viewx.dao.UserDetailMapper userDetailMapper;

    @Override
    @Transactional
    public Result<Long> uploadImage(Long userId, MultipartFile imageFile, ContentUploadDTO dto) {
        try {
            // 验证文件类型
            if (!isImageFile(imageFile)) {
                return Result.badRequest("只支持图片文件 (jpg, jpeg, png, gif, webp)");
            }

            // 验证文件大小 (最大10MB)
            if (imageFile.getSize() > 10 * 1024 * 1024) {
                return Result.badRequest("图片大小不能超过10MB");
            }

            // 1. 先创建内容记录以获取contentId
            Content content = new Content();
            content.setContentType("IMAGE");
            content.setTitle(dto.getTitle());
            content.setDescription(dto.getDescription());
            content.setUploaderId(userId);
            content.setCategory(dto.getCategory());
            content.setSubcategory(dto.getSubcategory());
            content.setVisibility(dto.getVisibility());
            content.setStatus("APPROVED");
            content.setCreatedAt(LocalDateTime.now());
            content.setUpdatedAt(LocalDateTime.now());
            content.setPublishedAt(LocalDateTime.now());

            if (dto.getTags() != null) {
                content.setTags(Arrays.asList(dto.getTags()));
            }

            contentMapper.insert(content);
            Long contentId = content.getId();

            // 2. 使用FilePathUtil生成图片路径
            String extension = FilePathUtil.extractExtension(imageFile.getOriginalFilename());
            if (extension.isEmpty()) {
                extension = ".jpg";
            }

            String imagePath = FilePathUtil.generatePostImagePath(userId, contentId, 1, extension);
            String storedFilename = storageStrategy.storeFile(imageFile, imagePath);
            String imageUrl = storageStrategy.getFileUrl(storedFilename);

            log.info("图片已上传: userId={}, contentId={}, path={}", userId, contentId, imagePath);

            // 3. 生成缩略图（可选）
            String thumbnailUrl = imageUrl; // 默认使用原图

            // 4. 更新内容记录
            content.setPrimaryUrl(imageUrl);
            content.setCoverUrl(imageUrl);
            content.setThumbnailUrl(thumbnailUrl);
            contentMapper.updateById(content);

            log.info("用户 {} 上传图片成功，ID: {}, path: {}", userId, contentId, imagePath);

            // 5. 提取并关联话题
            extractAndAssociateTopics(contentId, dto.getTitle(), dto.getDescription());

            return Result.success(contentId);

        } catch (Exception e) {
            log.error("图片上传失败", e);
            return Result.serverError("图片上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Long> uploadImageSet(Long userId, List<MultipartFile> imageFiles, ContentUploadDTO dto) {
        try {
            // 验证图片数量 (2-9张)
            if (imageFiles == null || imageFiles.size() < 2) {
                return Result.badRequest("图片集至少需要2张图片");
            }
            if (imageFiles.size() > 9) {
                return Result.badRequest("图片集最多支持9张图片");
            }

            // 验证所有文件
            for (MultipartFile file : imageFiles) {
                if (!isImageFile(file)) {
                    return Result.badRequest("只支持图片文件 (jpg, jpeg, png, gif, webp)");
                }
                if (file.getSize() > 10 * 1024 * 1024) {
                    return Result.badRequest("单张图片大小不能超过10MB");
                }
            }

            // 1. 先创建内容记录以获取contentId
            Content content = new Content();
            content.setContentType("IMAGE_SET");
            content.setTitle(dto.getTitle());
            content.setDescription(dto.getDescription());
            content.setUploaderId(userId);
            content.setCategory(dto.getCategory());
            content.setSubcategory(dto.getSubcategory());
            content.setVisibility(dto.getVisibility());
            content.setStatus("APPROVED");
            content.setCreatedAt(LocalDateTime.now());
            content.setUpdatedAt(LocalDateTime.now());
            content.setPublishedAt(LocalDateTime.now());

            if (dto.getTags() != null) {
                content.setTags(Arrays.asList(dto.getTags()));
            }

            contentMapper.insert(content);
            Long contentId = content.getId();

            // 2. 使用FilePathUtil上传所有图片
            List<String> imageUrls = new ArrayList<>();
            for (int i = 0; i < imageFiles.size(); i++) {
                MultipartFile file = imageFiles.get(i);
                String extension = FilePathUtil.extractExtension(file.getOriginalFilename());
                if (extension.isEmpty()) {
                    extension = ".jpg";
                }

                String imagePath = FilePathUtil.generatePostImagePath(userId, contentId, i + 1, extension);
                String storedFilename = storageStrategy.storeFile(file, imagePath);
                String imageUrl = storageStrategy.getFileUrl(storedFilename);
                imageUrls.add(imageUrl);

                log.info("图片集第{}张已上传: path={}", i + 1, imagePath);
            }

            // 3. 使用第一张图片作为封面和缩略图
            String coverUrl = imageUrls.get(0);
            String thumbnailUrl = coverUrl;

            // 4. 更新内容记录
            content.setPrimaryUrl(imageUrls.get(0));
            content.setMediaUrls(imageUrls);
            content.setCoverUrl(coverUrl);
            content.setThumbnailUrl(thumbnailUrl);
            contentMapper.updateById(content);

            log.info("用户 {} 上传图片集成功，ID: {}, 图片数: {}", userId, contentId, imageUrls.size());

            // 5. 提取并关联话题
            extractAndAssociateTopics(contentId, dto.getTitle(), dto.getDescription());

            return Result.success(contentId);

        } catch (Exception e) {
            log.error("图片集上传失败", e);
            return Result.serverError("图片集上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result<ContentDetailVO> getContentDetail(Long contentId, Long userId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null || content.getIsDeleted()) {
            return Result.notFound("内容不存在");
        }

        // 检查可见性
        if ("PRIVATE".equals(content.getVisibility())) {
            if (userId == null || !userId.equals(content.getUploaderId())) {
                return Result.forbidden("该内容为私有内容");
            }
        }

        // 增加浏览量
        content.setViewCount(content.getViewCount() + 1);
        contentMapper.updateById(content);

        ContentDetailVO vo = new ContentDetailVO();
        BeanUtils.copyProperties(content, vo);

        // 填充上传者信息
        User uploader = userMapper.selectById(content.getUploaderId());
        if (uploader != null) {
            vo.setUploaderNickname(uploader.getNickname());

            // 获取头像
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.flowbrain.viewx.pojo.entity.UserDetail> query = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            query.eq("user_id", content.getUploaderId());
            com.flowbrain.viewx.pojo.entity.UserDetail detail = userDetailMapper.selectOne(query);

            if (detail != null && detail.getAvatarUrl() != null) {
                if (!detail.getAvatarUrl().startsWith("http")) {
                    vo.setUploaderAvatar(storageStrategy.getFileUrl(detail.getAvatarUrl()));
                } else {
                    vo.setUploaderAvatar(detail.getAvatarUrl());
                }
            }
        }

        // 填充交互状态
        if (userId != null) {
            vo.setIsLiked(interactionService.isLiked(userId, contentId));
            vo.setIsFavorited(interactionService.isFavorited(userId, contentId));
        } else {
            vo.setIsLiked(false);
            vo.setIsFavorited(false);
            vo.setIsFollowingUploader(false);
        }

        return Result.success(vo);
    }

    @Override
    public Result<List<ContentVO>> getUserContents(Long userId, String contentType) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Content> query = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();

        query.eq("uploader_id", userId);

        if (contentType != null && !contentType.isEmpty()) {
            query.eq("content_type", contentType);
        }

        query.orderByDesc("created_at");

        List<Content> contents = contentMapper.selectList(query);

        List<ContentVO> voList = contents.stream().map(content -> {
            ContentVO vo = new ContentVO();
            BeanUtils.copyProperties(content, vo);

            // 图片集特殊处理
            if ("IMAGE_SET".equals(content.getContentType()) && content.getMediaUrls() != null) {
                vo.setImageCount(content.getMediaUrls().size());
            }

            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    @Override
    public Result<String> deleteContent(Long userId, Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            return Result.notFound("内容不存在");
        }

        if (!content.getUploaderId().equals(userId)) {
            return Result.forbidden("无权删除此内容");
        }

        contentMapper.deleteById(contentId);
        return Result.success("删除成功");
    }

    // 辅助方法

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp"));
    }

    private void extractAndAssociateTopics(Long contentId, String title, String description) {
        try {
            Set<String> topics = new HashSet<>();

            if (title != null) {
                topics.addAll(topicService.extractTopicsFromText(title));
            }

            if (description != null) {
                topics.addAll(topicService.extractTopicsFromText(description));
            }

            if (!topics.isEmpty()) {
                topicService.associateTopicsWithVideo(contentId, topics);
                log.info("内容 {} 关联了 {} 个话题: {}", contentId, topics.size(), topics);
            }
        } catch (Exception e) {
            log.warn("话题提取失败: {}", e.getMessage());
        }
    }
}
