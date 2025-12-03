package com.flowbrain.viewx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.dao.UserMapper;
import com.flowbrain.viewx.dao.VideoMapper;
import com.flowbrain.viewx.pojo.dto.VideoCreateDTO;
import com.flowbrain.viewx.pojo.dto.VideoUpdateDTO;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.entity.Video;
import com.flowbrain.viewx.pojo.vo.VideoDetailVO;
import com.flowbrain.viewx.service.InteractionService;
import com.flowbrain.viewx.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

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
            vo.setUploaderNickname(uploader.getNickname());
            // 假设 UserDetail 中有 avatar，这里简化处理，实际可能需要关联查询
            // vo.setUploaderAvatar(uploader.getAvatar()); 
            // 由于 User 实体可能没有 avatar 字段 (在 UserDetail 中)，这里暂时留空或需要额外查询
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
    public Result<Long> createVideo(Long userId, VideoCreateDTO dto) {
        Video video = new Video();
        BeanUtils.copyProperties(dto, video);
        
        video.setUploaderId(userId);
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        video.setPublishedAt(LocalDateTime.now()); // 默认立即发布
        video.setStatus("APPROVED"); // 默认审核通过，实际应为 PENDING
        
        // 处理标签数组转List
        if (dto.getTags() != null) {
            video.setTags(Arrays.asList(dto.getTags()));
        }

        videoMapper.insert(video);
        log.info("用户 {} 创建视频成功，ID: {}", userId, video.getId());
        return Result.success(video.getId());
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
        if (dto.getTitle() != null) { video.setTitle(dto.getTitle()); changed = true; }
        if (dto.getDescription() != null) { video.setDescription(dto.getDescription()); changed = true; }
        if (dto.getThumbnailUrl() != null) { video.setThumbnailUrl(dto.getThumbnailUrl()); changed = true; }
        if (dto.getCategory() != null) { video.setCategory(dto.getCategory()); changed = true; }
        if (dto.getSubcategory() != null) { video.setSubcategory(dto.getSubcategory()); changed = true; }
        if (dto.getVisibility() != null) { video.setVisibility(dto.getVisibility()); changed = true; }
        if (dto.getTags() != null) { video.setTags(Arrays.asList(dto.getTags())); changed = true; }

        if (changed) {
            video.setUpdatedAt(LocalDateTime.now());
            videoMapper.updateById(video);
            return Result.success("视频更新成功");
        }
        
        return Result.success("无变更");
    }

    @Override
    public Result<String> uploadVideoFile(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : ".mp4";
            
            String filename = "video_" + UUID.randomUUID() + extension;
            String storedFilename = storageStrategy.storeFile(file, filename);
            String url = storageStrategy.getFileUrl(storedFilename);
            
            return Result.success(url);
        } catch (Exception e) {
            log.error("视频上传失败", e);
            return Result.error(500, "视频上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> uploadCoverImage(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : ".jpg";
            
            String filename = "cover_" + UUID.randomUUID() + extension;
            String storedFilename = storageStrategy.storeFile(file, filename);
            String url = storageStrategy.getFileUrl(storedFilename);
            
            return Result.success(url);
        } catch (Exception e) {
            log.error("封面上传失败", e);
            return Result.error(500, "封面上传失败: " + e.getMessage());
        }
    }
}
