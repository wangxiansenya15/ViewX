package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.dao.UserMapper;
import com.flowbrain.viewx.dao.VideoMapper;
import com.flowbrain.viewx.pojo.dto.VideoUploadDTO;
import com.flowbrain.viewx.pojo.dto.VideoUpdateDTO;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.entity.Video;
import com.flowbrain.viewx.pojo.vo.VideoDetailVO;
import com.flowbrain.viewx.service.InteractionService;
import com.flowbrain.viewx.service.TopicService;
import com.flowbrain.viewx.service.VideoService;
import com.flowbrain.viewx.service.VideoProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class VideoServiceImpl implements VideoService {

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LocalStorageStrategy storageStrategy;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private VideoProcessingService videoProcessingService;

    @Autowired
    private com.flowbrain.viewx.dao.UserDetailMapper userDetailMapper;

    @Override
    public Result<VideoDetailVO> getVideoDetail(Long videoId, Long userId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null || video.getIsDeleted()) {
            return Result.error(404, "视频不存在");
        }

        // 检查可见性
        if ("PRIVATE".equals(video.getVisibility())) {
            if (userId == null || !userId.equals(video.getUploaderId())) {
                return Result.error(403, "该视频为私有视频");
            }
        }

        // 增加播放量 (简单实现，实际应异步或使用Redis)
        video.setViewCount(video.getViewCount() + 1);
        videoMapper.updateById(video);

        VideoDetailVO vo = new VideoDetailVO();
        BeanUtils.copyProperties(video, vo);

        // 填充上传者信息
        User uploader = userMapper.selectById(video.getUploaderId());
        if (uploader != null) {
            if (uploader.getNickname() != null && !uploader.getNickname().isEmpty()) {
                vo.setUploaderNickname(uploader.getNickname());
            } else {
                vo.setUploaderNickname(uploader.getUsername());
            }

            // 获取 UserDetail (avatar)
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.flowbrain.viewx.pojo.entity.UserDetail> query = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            query.eq("user_id", video.getUploaderId());
            com.flowbrain.viewx.pojo.entity.UserDetail detail = userDetailMapper.selectOne(query);

            if (detail != null && detail.getAvatar() != null) {
                // 处理头像URL
                if (!detail.getAvatar().startsWith("http")) {
                    vo.setUploaderAvatar(storageStrategy.getFileUrl(detail.getAvatar()));
                } else {
                    vo.setUploaderAvatar(detail.getAvatar());
                }
            }
        }

        // 填充交互状态
        if (userId != null) {
            vo.setIsLiked(interactionService.isLiked(userId, videoId));
            vo.setIsFavorited(interactionService.isFavorited(userId, videoId));
            // vo.setIsFollowingUploader(...) // 需要 FollowService
        } else {
            vo.setIsLiked(false);
            vo.setIsFavorited(false);
            vo.setIsFollowingUploader(false);
        }

        return Result.success(vo);
    }

    @Override
    @Transactional
    public Result<Long> uploadVideo(Long userId, MultipartFile videoFile, MultipartFile coverFile, VideoUploadDTO dto) {
        try {
            // 1. 上传视频文件到 /videos 目录
            String originalFilename = videoFile.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".mp4";

            String filename = "video_" + userId + "_" + System.currentTimeMillis() + extension;
            String videoFilename = "videos/" + filename;
            String storedFilename = storageStrategy.storeFile(videoFile, videoFilename);
            String videoUrl = storageStrategy.getFileUrl(storedFilename);

            // 2. 处理封面图片上传 (如果提供了封面文件)
            String coverUrl = null;
            String thumbnailUrl = null;
            
            if (coverFile != null && !coverFile.isEmpty()) {
                try {
                    // 上传封面图片
                    String coverOriginalFilename = coverFile.getOriginalFilename();
                    String coverExtension = coverOriginalFilename != null && coverOriginalFilename.contains(".")
                            ? coverOriginalFilename.substring(coverOriginalFilename.lastIndexOf("."))
                            : ".jpg";

                    String coverFilename = "cover_" + userId + "_" + System.currentTimeMillis() + coverExtension;
                    String coverPath = "videos/covers/" + coverFilename;
                    String storedCoverFilename = storageStrategy.storeFile(coverFile, coverPath);
                    coverUrl = storageStrategy.getFileUrl(storedCoverFilename);

                    // 生成缩略图
                    try {
                        byte[] thumbnailBytes = videoProcessingService.generateThumbnailFromCover(coverFile);
                        String thumbnailFilename = "videos/thumbnails/thumb_" + userId + "_" + System.currentTimeMillis() + ".jpg";
                        String storedThumbnailFilename = storageStrategy.storeFile(
                                new java.io.ByteArrayInputStream(thumbnailBytes),
                                thumbnailFilename);
                        thumbnailUrl = storageStrategy.getFileUrl(storedThumbnailFilename);
                        log.info("成功生成缩略图: {}", thumbnailUrl);
                    } catch (Exception e) {
                        log.warn("缩略图生成失败，使用封面图作为缩略图: {}", e.getMessage());
                        thumbnailUrl = coverUrl; // 降级处理
                    }
                    
                    log.info("封面上传成功: coverUrl={}, thumbnailUrl={}", coverUrl, thumbnailUrl);
                } catch (Exception e) {
                    log.error("封面上传失败，继续处理视频上传", e);
                    // 封面上传失败不影响视频上传
                }
            } else if (dto.getCoverUrl() != null && !dto.getCoverUrl().isEmpty()) {
                // 如果没有上传封面文件，但DTO中有coverUrl（兼容旧逻辑）
                if (!dto.getCoverUrl().startsWith("http")) {
                    coverUrl = storageStrategy.getFileUrl(dto.getCoverUrl());
                } else {
                    coverUrl = dto.getCoverUrl();
                }
                thumbnailUrl = dto.getThumbnailUrl();
            }

            // 3. 创建视频记录
            Video video = new Video();
            BeanUtils.copyProperties(dto, video);

            video.setVideoUrl(videoUrl);
            video.setUploaderId(userId);
            video.setCreatedAt(LocalDateTime.now());
            video.setUpdatedAt(LocalDateTime.now());
            video.setPublishedAt(LocalDateTime.now()); // 默认立即发布
            video.setStatus("APPROVED"); // 默认审核通过，实际应为 PENDING

            // 设置封面和缩略图
            if (coverUrl != null) {
                video.setCoverUrl(coverUrl);
            }
            if (thumbnailUrl != null) {
                video.setThumbnailUrl(thumbnailUrl);
            }

            // 处理标签数组转List
            if (dto.getTags() != null) {
                video.setTags(Arrays.asList(dto.getTags()));
            }

            videoMapper.insert(video);
            log.info("用户 {} 上传视频成功，ID: {}, VideoURL: {}, CoverURL: {}, ThumbnailURL: {}", 
                    userId, video.getId(), videoUrl, coverUrl, thumbnailUrl);

            // 4. 提取并关联话题
            Set<String> topics = new HashSet<>();

            // 从标题中提取话题
            if (dto.getTitle() != null) {
                topics.addAll(topicService.extractTopicsFromText(dto.getTitle()));
            }

            // 从描述中提取话题
            if (dto.getDescription() != null) {
                topics.addAll(topicService.extractTopicsFromText(dto.getDescription()));
            }

            // 关联话题
            if (!topics.isEmpty()) {
                topicService.associateTopicsWithVideo(video.getId(), topics);
                log.info("视频 {} 关联了 {} 个话题: {}", video.getId(), topics.size(), topics);
            }

            return Result.success(video.getId());

        } catch (Exception e) {
            log.error("视频上传失败", e);
            return Result.error(500, "视频上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<String> updateVideo(Long userId, Long videoId, VideoUpdateDTO dto) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return Result.error(404, "视频不存在");
        }

        if (!video.getUploaderId().equals(userId)) {
            return Result.error(403, "无权修改此视频");
        }

        boolean changed = false;
        if (dto.getTitle() != null) {
            video.setTitle(dto.getTitle());
            changed = true;
        }
        if (dto.getDescription() != null) {
            video.setDescription(dto.getDescription());
            changed = true;
        }
        if (dto.getThumbnailUrl() != null) {
            video.setThumbnailUrl(dto.getThumbnailUrl());
            changed = true;
        }
        if (dto.getCategory() != null) {
            video.setCategory(dto.getCategory());
            changed = true;
        }
        if (dto.getSubcategory() != null) {
            video.setSubcategory(dto.getSubcategory());
            changed = true;
        }
        if (dto.getVisibility() != null) {
            video.setVisibility(dto.getVisibility());
            changed = true;
        }
        if (dto.getTags() != null) {
            video.setTags(Arrays.asList(dto.getTags()));
            changed = true;
        }

        if (changed) {
            video.setUpdatedAt(LocalDateTime.now());
            videoMapper.updateById(video);
            return Result.success("视频更新成功");
        }

        return Result.success("无变更");
    }

    @Override
    public Result<com.flowbrain.viewx.pojo.vo.CoverUploadVO> uploadCoverImage(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";

            // 1. 上传原始封面图
            String filename = "cover_" + System.currentTimeMillis() + extension;
            String coverFilename = "videos/covers/" + filename;
            String storedFilename = storageStrategy.storeFile(file, coverFilename);
            String coverUrl = storageStrategy.getFileUrl(storedFilename);

            // 2. 生成并上传缩略图（320x180）
            String thumbnailUrl = coverUrl; // 默认使用封面图
            try {
                byte[] thumbnailBytes = videoProcessingService.generateThumbnailFromCover(file);
                String thumbnailFilename = "videos/thumbnails/thumb_" + System.currentTimeMillis() + ".jpg";

                String storedThumbnailFilename = storageStrategy.storeFile(
                        new java.io.ByteArrayInputStream(thumbnailBytes),
                        thumbnailFilename);
                thumbnailUrl = storageStrategy.getFileUrl(storedThumbnailFilename);

                log.info("成功生成缩略图: {}", thumbnailUrl);
            } catch (Exception e) {
                log.warn("缩略图生成失败，将使用原始封面: {}", e.getMessage());
                // 缩略图生成失败不影响封面上传
            }

            com.flowbrain.viewx.pojo.vo.CoverUploadVO vo = new com.flowbrain.viewx.pojo.vo.CoverUploadVO(coverUrl, thumbnailUrl);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("封面上传失败", e);
            return Result.error(500, "封面上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> deleteVideo(Long userId, Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return Result.error(404, "视频不存在");
        }

        // 权限检查：必须是上传者才能删除
        if (!video.getUploaderId().equals(userId)) {
            return Result.error(403, "无权删除此视频");
        }

        videoMapper.deleteById(videoId);
        return Result.success("删除成功");
    }

    @Override
    public Result<java.util.List<Video>> getMyVideos(Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Video> query = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        query.eq("uploader_id", userId)
                .orderByDesc("created_at");
        java.util.List<Video> list = videoMapper.selectList(query);
        return Result.success(list);
    }
}
