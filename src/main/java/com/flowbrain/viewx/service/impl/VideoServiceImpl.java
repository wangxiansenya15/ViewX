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

    @Autowired
    private com.flowbrain.viewx.service.RecommendService recommendService;

    @Override
    public Result<VideoDetailVO> getVideoDetail(Long videoId, Long userId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null || video.getIsDeleted()) {
            return Result.notFound("视频不存在");
        }

        // 检查可见性
        if ("PRIVATE".equals(video.getVisibility())) {
            if (userId == null || !userId.equals(video.getUploaderId())) {
                return Result.forbidden("该视频为私有视频");
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

            if (detail != null && detail.getAvatarUrl() != null) {
                // 处理头像URL
                if (!detail.getAvatarUrl().startsWith("http")) {
                    vo.setUploaderAvatar(storageStrategy.getFileUrl(detail.getAvatarUrl()));
                } else {
                    vo.setUploaderAvatar(detail.getAvatarUrl());
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
                // 用户上传了封面图
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
                        String thumbnailFilename = "videos/thumbnails/thumb_" + userId + "_"
                                + System.currentTimeMillis() + ".jpg";
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
            } else {
                // 用户既没有上传封面，也没有提供coverUrl，自动从视频提取关键帧
                log.info("用户未上传封面图，尝试从视频中提取关键帧作为封面");
                try {
                    // 获取已上传的视频文件
                    java.io.File uploadedVideoFile = new java.io.File(
                            storageStrategy.getStorageRoot() + "/" + storedFilename);

                    if (uploadedVideoFile.exists()) {
                        // 从视频第1秒提取关键帧作为封面
                        String generatedCoverFilename = videoProcessingService.generateThumbnail(uploadedVideoFile, 1);

                        // 生成的文件在 uploads/videos 目录下，需要移动到 covers 目录
                        java.io.File generatedFile = new java.io.File(
                                storageStrategy.getStorageRoot() + "/videos/" + generatedCoverFilename);

                        if (generatedFile.exists()) {
                            // 构建封面URL
                            coverUrl = storageStrategy.getFileUrl("videos/" + generatedCoverFilename);

                            // 从生成的封面创建缩略图
                            try {
                                // 直接读取生成的封面文件
                                java.io.FileInputStream fis = new java.io.FileInputStream(generatedFile);
                                byte[] coverBytes = fis.readAllBytes();
                                fis.close();

                                // 创建临时的 MultipartFile 实现
                                final String finalCoverFilename = generatedCoverFilename;
                                MultipartFile tempCoverFile = new MultipartFile() {
                                    @Override
                                    public String getName() {
                                        return "cover";
                                    }

                                    @Override
                                    public String getOriginalFilename() {
                                        return finalCoverFilename;
                                    }

                                    @Override
                                    public String getContentType() {
                                        return "image/jpeg";
                                    }

                                    @Override
                                    public boolean isEmpty() {
                                        return coverBytes.length == 0;
                                    }

                                    @Override
                                    public long getSize() {
                                        return coverBytes.length;
                                    }

                                    @Override
                                    public byte[] getBytes() {
                                        return coverBytes;
                                    }

                                    @Override
                                    public java.io.InputStream getInputStream() {
                                        return new java.io.ByteArrayInputStream(coverBytes);
                                    }

                                    @Override
                                    public void transferTo(java.io.File dest) throws java.io.IOException {
                                        java.nio.file.Files.write(dest.toPath(), coverBytes);
                                    }
                                };

                                byte[] thumbnailBytes = videoProcessingService
                                        .generateThumbnailFromCover(tempCoverFile);
                                String thumbnailFilename = "videos/thumbnails/thumb_" + userId + "_"
                                        + System.currentTimeMillis() + ".jpg";
                                String storedThumbnailFilename = storageStrategy.storeFile(
                                        new java.io.ByteArrayInputStream(thumbnailBytes),
                                        thumbnailFilename);
                                thumbnailUrl = storageStrategy.getFileUrl(storedThumbnailFilename);

                                log.info("从视频提取的封面成功生成缩略图: {}", thumbnailUrl);
                            } catch (Exception e) {
                                log.warn("从提取的封面生成缩略图失败，使用封面作为缩略图: {}", e.getMessage());
                                thumbnailUrl = coverUrl;
                            }

                            log.info("成功从视频提取封面: coverUrl={}, thumbnailUrl={}", coverUrl, thumbnailUrl);
                        } else {
                            log.warn("FFmpeg 生成的封面文件不存在: {}", generatedFile.getAbsolutePath());
                        }
                    } else {
                        log.warn("上传的视频文件不存在，无法提取封面: {}", uploadedVideoFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    log.error("从视频提取封面失败，视频将没有封面图", e);
                    // 提取失败不影响视频上传，只是没有封面而已
                }
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

            // 5. 更新推荐系统的热度分数，使新视频立即出现在首页
            try {
                recommendService.updateVideoScore(video.getId());
                log.info("视频 {} 已加入推荐列表", video.getId());
            } catch (Exception e) {
                log.warn("更新推荐分数失败，不影响视频上传: {}", e.getMessage());
            }

            return Result.success(video.getId());

        } catch (Exception e) {
            log.error("视频上传失败", e);
            return Result.serverError("视频上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<String> updateVideo(Long userId, Long videoId, VideoUpdateDTO dto) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return Result.notFound("视频不存在");
        }

        if (!video.getUploaderId().equals(userId)) {
            return Result.forbidden("无权修改此视频");
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

            com.flowbrain.viewx.pojo.vo.CoverUploadVO vo = new com.flowbrain.viewx.pojo.vo.CoverUploadVO(coverUrl,
                    thumbnailUrl);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("封面上传失败", e);
            return Result.serverError("封面上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> deleteVideo(Long userId, Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return Result.notFound("视频不存在");
        }

        // 权限检查：必须是上传者才能删除
        if (!video.getUploaderId().equals(userId)) {
            return Result.forbidden("无权删除此视频");
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
