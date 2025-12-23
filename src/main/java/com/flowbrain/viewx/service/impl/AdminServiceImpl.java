package com.flowbrain.viewx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flowbrain.viewx.common.EventType;
import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.dao.UserDetailMapper;
import com.flowbrain.viewx.dao.UserMapper;
import com.flowbrain.viewx.dao.VideoMapper;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.entity.UserDetail;
import com.flowbrain.viewx.pojo.entity.Video;
import com.flowbrain.viewx.pojo.vo.VideoReviewVO;
import com.flowbrain.viewx.service.AdminService;
import com.flowbrain.viewx.service.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员服务实现类
 */
@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserDetailMapper userDetailMapper;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private LocalStorageStrategy storageStrategy;

    @Autowired
    private com.flowbrain.viewx.service.RecommendService recommendService;

    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Override
    public Result<List<VideoReviewVO>> getPendingVideos(int page, int size) {
        try {
            // 查询待审核的视频
            Page<Video> videoPage = new Page<>(page, size);
            QueryWrapper<Video> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", "PENDING")
                    .orderByDesc("created_at");

            Page<Video> result = videoMapper.selectPage(videoPage, queryWrapper);
            List<VideoReviewVO> voList = convertToVideoReviewVOList(result.getRecords());

            log.info("获取待审核视频列表成功，共 {} 条", voList.size());
            return Result.success(voList);
        } catch (Exception e) {
            log.error("获取待审核视频列表失败", e);
            return Result.serverError("获取待审核视频列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<VideoReviewVO>> getAllVideos(String status, int page, int size) {
        try {
            Page<Video> videoPage = new Page<>(page, size);
            QueryWrapper<Video> queryWrapper = new QueryWrapper<>();

            // 如果指定了状态，则过滤
            if (status != null && !status.isEmpty()) {
                queryWrapper.eq("status", status);
            }

            queryWrapper.orderByDesc("created_at");

            Page<Video> result = videoMapper.selectPage(videoPage, queryWrapper);
            List<VideoReviewVO> voList = convertToVideoReviewVOList(result.getRecords());

            log.info("获取视频列表成功，状态: {}, 共 {} 条", status, voList.size());
            return Result.success(voList);
        } catch (Exception e) {
            log.error("获取视频列表失败", e);
            return Result.serverError("获取视频列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<String> approveVideo(Long videoId) {
        try {
            Video video = videoMapper.selectById(videoId);
            if (video == null) {
                return Result.notFound("视频不存在");
            }

            if ("APPROVED".equals(video.getStatus())) {
                return Result.badRequest("视频已经通过审核");
            }

            // 更新视频状态为已通过
            video.setStatus("APPROVED");
            video.setPublishedAt(LocalDateTime.now());
            video.setUpdatedAt(LocalDateTime.now());
            videoMapper.updateById(video);

            // 更新 Redis 推荐分数
            try {
                recommendService.updateVideoScore(videoId);
                log.info("已更新视频 {} 的推荐分数", videoId);
            } catch (Exception e) {
                log.warn("更新推荐分数失败，不影响审核: {}", e.getMessage());
            }

            // 清除分页缓存，确保前端能立即看到新视频
            clearTrendingCache();

            // 发布视频审核通过事件（用于通知用户）
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("videoId", videoId);
            eventPublisher.publishEvent(EventType.VIDEO_APPROVED, video.getUploaderId(), eventData);

            log.info("视频审核通过，视频ID: {}, 上传者ID: {}", videoId, video.getUploaderId());
            return Result.success("视频审核通过");
        } catch (Exception e) {
            log.error("视频审核通过失败，视频ID: {}", videoId, e);
            return Result.serverError("视频审核通过失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<String> rejectVideo(Long videoId, String reason) {
        try {
            Video video = videoMapper.selectById(videoId);
            if (video == null) {
                return Result.notFound("视频不存在");
            }

            if ("REJECTED".equals(video.getStatus())) {
                return Result.badRequest("视频已经被拒绝");
            }

            // 更新视频状态为已拒绝
            video.setStatus("REJECTED");
            video.setUpdatedAt(LocalDateTime.now());
            videoMapper.updateById(video);

            // 发布视频审核拒绝事件（用于通知用户）
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("videoId", videoId);
            eventData.put("reason", reason);
            eventPublisher.publishEvent(EventType.VIDEO_REJECTED, video.getUploaderId(), eventData);

            log.info("视频审核拒绝，视频ID: {}, 上传者ID: {}, 原因: {}", videoId, video.getUploaderId(), reason);
            return Result.success("视频已拒绝");
        } catch (Exception e) {
            log.error("视频审核拒绝失败，视频ID: {}", videoId, e);
            return Result.serverError("视频审核拒绝失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<String> deleteVideo(Long videoId) {
        try {
            Video video = videoMapper.selectById(videoId);
            if (video == null) {
                return Result.notFound("视频不存在");
            }

            // 软删除
            videoMapper.deleteById(videoId);

            log.info("管理员删除视频成功，视频ID: {}", videoId);
            return Result.success("视频已删除");
        } catch (Exception e) {
            log.error("删除视频失败，视频ID: {}", videoId, e);
            return Result.serverError("删除视频失败: " + e.getMessage());
        }
    }

    /**
     * 将 Video 实体列表转换为 VideoReviewVO 列表
     */
    private List<VideoReviewVO> convertToVideoReviewVOList(List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return new ArrayList<>();
        }

        return videos.stream().map(video -> {
            VideoReviewVO vo = new VideoReviewVO();
            BeanUtils.copyProperties(video, vo);

            // 处理标签
            if (video.getTags() != null && !video.getTags().isEmpty()) {
                vo.setTags(video.getTags().toArray(new String[0]));
            }

            // 填充上传者信息
            User uploader = userMapper.selectById(video.getUploaderId());
            if (uploader != null) {
                vo.setUploaderUsername(uploader.getUsername());
                vo.setUploaderNickname(
                        uploader.getNickname() != null ? uploader.getNickname() : uploader.getUsername());

                // 获取用户详情（头像）
                QueryWrapper<UserDetail> detailQuery = new QueryWrapper<>();
                detailQuery.eq("user_id", video.getUploaderId());
                UserDetail detail = userDetailMapper.selectOne(detailQuery);

                if (detail != null && detail.getAvatarUrl() != null) {
                    // 处理头像URL
                    if (!detail.getAvatarUrl().startsWith("http")) {
                        vo.setUploaderAvatar(storageStrategy.getFileUrl(detail.getAvatarUrl()));
                    } else {
                        vo.setUploaderAvatar(detail.getAvatarUrl());
                    }
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 清除 trending 视频的分页缓存
     * 当有新视频审核通过时调用，确保前端能立即看到更新
     */
    private void clearTrendingCache() {
        try {
            // 使用通配符删除所有 trending 分页缓存
            String pattern = com.flowbrain.viewx.common.RedisKeyConstants.Recommend.getTrendingKey() + ":page:*";
            java.util.Set<String> keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("已清除 {} 个 trending 缓存键", keys.size());
            } else {
                log.debug("没有找到需要清除的 trending 缓存");
            }
        } catch (Exception e) {
            log.warn("清除 trending 缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public Result<com.flowbrain.viewx.pojo.vo.DashboardStatsVO> getDashboardStats() {
        try {
            // 1. Users
            long totalUsers = userMapper.selectCount(new QueryWrapper<>());

            // 2. Videos
            long totalVideos = videoMapper.selectCount(new QueryWrapper<>());

            // 3. Storage & Views (Aggregation)
            QueryWrapper<Video> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("sum(file_size) as totalSize", "sum(view_count) as totalViews");

            // selectMaps returns List<Map<String, Object>>
            List<Map<String, Object>> result = videoMapper.selectMaps(queryWrapper);

            long storageUsed = 0;
            try {
                java.nio.file.Path path = java.nio.file.Paths.get("uploads");
                if (java.nio.file.Files.exists(path)) {
                    // Walk file tree to sum size
                    storageUsed = java.nio.file.Files.walk(path)
                            .filter(p -> p.toFile().isFile())
                            .mapToLong(p -> p.toFile().length())
                            .sum();
                }
            } catch (Exception e) {
                log.warn("Failed to calculate storage size of uploads", e);
            }

            long totalViews = 0;
            if (result != null && !result.isEmpty()) {
                Map<String, Object> map = result.get(0);
                if (map != null) {
                    if (map.get("totalViews") != null) {
                        Object viewsObj = map.get("totalViews");
                        if (viewsObj instanceof Number) {
                            totalViews = ((Number) viewsObj).longValue();
                        }
                    }
                }
            }

            com.flowbrain.viewx.pojo.vo.DashboardStatsVO stats = com.flowbrain.viewx.pojo.vo.DashboardStatsVO.builder()
                    .totalUsers(totalUsers)
                    .userTrend(12.5) // Mock
                    .totalVideos(totalVideos)
                    .videoTrend(8.2) // Mock
                    .totalViews(totalViews)
                    .viewTrend(2.4) // Mock
                    .storageUsed(storageUsed)
                    .storageTrend(5.1) // Mock
                    .build();

            return Result.success(stats);
        } catch (Exception e) {
            log.error("Failed to get dashboard stats", e);
            return Result.serverError("获取统计数据失败");
        }
    }

    @Override
    @Transactional
    public Result<Long> createUser(com.flowbrain.viewx.pojo.dto.AdminCreateUserDTO dto) {
        try {
            // 1. Check if username already exists
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", dto.getUsername());
            if (userMapper.selectCount(queryWrapper) > 0) {
                return Result.badRequest("用户名已存在");
            }

            // 2. Check if email already exists
            if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
                QueryWrapper<User> emailQuery = new QueryWrapper<>();
                emailQuery.eq("email", dto.getEmail());
                if (userMapper.selectCount(emailQuery) > 0) {
                    return Result.badRequest("邮箱已被使用");
                }
            }

            // 3. Create user
            User user = new User();
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            user.setPhone(dto.getPhone());
            user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());

            // Encrypt password
            String encryptedPassword = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                    .encode(dto.getPassword() != null ? dto.getPassword() : "123456");
            user.setPassword(encryptedPassword);

            // Set role
            com.flowbrain.viewx.common.enums.Role role = com.flowbrain.viewx.common.enums.Role.USER;
            if (dto.getRole() != null) {
                try {
                    role = com.flowbrain.viewx.common.enums.Role.valueOf(dto.getRole());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid role: {}, using default USER", dto.getRole());
                }
            }
            user.setRole(role);

            // Set account status (all enabled by default)
            user.setEnabled(true);
            user.setAccountNonExpired(true);
            user.setAccountNonLocked(true);
            user.setCredentialsNonExpired(true);

            userMapper.insert(user);

            // 4. Create user detail record
            UserDetail detail = new UserDetail();
            detail.setUserId(user.getId());
            userDetailMapper.insert(detail);

            log.info("管理员创建用户成功，用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
            return Result.success(user.getId());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("创建用户失败 - 数据完整性违规: {}", e.getMessage());
            // Check for common constraint violations
            if (e.getMessage().contains("username")) {
                return Result.badRequest("用户名已存在或格式不正确");
            } else if (e.getMessage().contains("email")) {
                return Result.badRequest("邮箱已存在或格式不正确");
            }
            return Result.badRequest("创建用户失败，请检查输入数据");
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return Result.serverError("创建用户失败，请稍后重试");
        }
    }

    @Override
    @Transactional
    public Result<String> deleteUser(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return Result.notFound("用户不存在");
            }

            // Soft delete
            userMapper.deleteById(userId);

            log.info("管理员删除用户成功，用户ID: {}, 用户名: {}", userId, user.getUsername());
            return Result.success("用户已删除");
        } catch (Exception e) {
            log.error("删除用户失败，用户ID: {}", userId, e);
            return Result.serverError("删除用户失败，请稍后重试");
        }
    }
}
