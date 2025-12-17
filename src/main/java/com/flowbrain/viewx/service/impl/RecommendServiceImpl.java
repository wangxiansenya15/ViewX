package com.flowbrain.viewx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.flowbrain.viewx.common.RedisKeyConstants;
import com.flowbrain.viewx.common.Result;

import com.flowbrain.viewx.dao.UserDetailMapper;
import com.flowbrain.viewx.dao.UserMapper;
import com.flowbrain.viewx.dao.VideoMapper;
import com.flowbrain.viewx.pojo.entity.UserDetail;
import com.flowbrain.viewx.pojo.entity.Video;
import com.flowbrain.viewx.service.RecommendService;
import com.flowbrain.viewx.service.StorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserDetailMapper userDetailMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StorageStrategy storageStrategy;

    @Override
    public List<com.flowbrain.viewx.pojo.vo.VideoListVO> getTrendingVideos(int page, int size) {
        int offset = (page - 1) * size;
        String cacheKey = RedisKeyConstants.Recommend.getTrendingKey() + ":page:" + page + ":size:" + size;

        // 1. 尝试从 Redis 缓存获取
        @SuppressWarnings("unchecked")
        List<Long> cachedVideoIds = (List<Long>) redisTemplate.opsForValue().get(cacheKey);

        List<Video> videos;

        if (cachedVideoIds != null && !cachedVideoIds.isEmpty()) {
            // 缓存命中
            log.info("Cache HIT for page: {}, size: {} - {} video IDs found", page, size, cachedVideoIds.size());
            videos = cachedVideoIds.stream()
                    .map(videoMapper::selectById)
                    .filter(v -> v != null)
                    .collect(Collectors.toList());
        } else {
            // 缓存未命中，查询数据库
            log.info("Cache MISS - Fetching videos from database (Testing Mode) - page: {}, size: {}", page, size);
            videos = videoMapper.selectLatestVideos(offset, size);

            if (videos.isEmpty()) {
                log.warn("No approved videos found in database");
            } else {
                log.info("Found {} videos from database, caching result", videos.size());

                // 2. 将查询结果缓存到 Redis（只缓存视频 ID 列表）
                List<Long> videoIds = videos.stream()
                        .map(Video::getId)
                        .collect(Collectors.toList());

                // 缓存 5 分钟（300 秒）
                redisTemplate.opsForValue().set(cacheKey, videoIds, java.time.Duration.ofMinutes(5));
                log.info("Cached {} video IDs with key: {}", videoIds.size(), cacheKey);
            }
        }

        // 转换为 VO 并填充用户信息
        return videos.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    private com.flowbrain.viewx.pojo.vo.VideoListVO convertToVO(Video video) {
        com.flowbrain.viewx.pojo.vo.VideoListVO vo = new com.flowbrain.viewx.pojo.vo.VideoListVO();
        org.springframework.beans.BeanUtils.copyProperties(video, vo);

        // 填充用户信息
        if (video.getUploaderId() != null) {
            // 获取 User (nickname)
            com.flowbrain.viewx.pojo.entity.User uploader = userMapper.selectById(video.getUploaderId());
            if (uploader != null) {
                if (uploader.getNickname() != null && !uploader.getNickname().isEmpty()) {
                    vo.setUploaderNickname(uploader.getNickname());
                } else {
                    vo.setUploaderNickname(uploader.getUsername());
                }
            }

            // 获取 UserDetail (avatar)
            QueryWrapper<UserDetail> query = new QueryWrapper<>();
            query.eq("user_id", video.getUploaderId());
            UserDetail detail = userDetailMapper.selectOne(query);

            if (detail != null) {
                if (detail.getAvatar() != null) {
                    // 处理头像URL
                    if (!detail.getAvatar().startsWith("http")) {
                        String fullUrl = storageStrategy.getFileUrl(detail.getAvatar());
                        vo.setUploaderAvatar(fullUrl);
                    } else {
                        vo.setUploaderAvatar(detail.getAvatar());
                    }
                }
            }
        }
        return vo;
    }

    @Override
    public List<com.flowbrain.viewx.pojo.vo.VideoListVO> getRecommendedVideos(Long userId, int page, int size) {
        // 1. If user is guest or no preferences, return trending
        if (userId == null) {
            return getTrendingVideos(page, size);
        }

        // TODO: Implement personalized recommendation
        return getTrendingVideos(page, size);
    }

    @Override
    public void updateVideoScore(Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null)
            return;

        double score = calculateScore(video);

        String trendingKey = RedisKeyConstants.Recommend.getTrendingKey();
        redisTemplate.opsForZSet().add(trendingKey, videoId.toString(), score);
        log.debug("Updated score for video {}: {}", videoId, score);
    }

    /**
     * Initialize recommendation scores for all approved videos.
     * This should be called on application startup or manually to populate Redis.
     */
    public void initializeRecommendationScores() {
        log.info("Initializing recommendation scores for all approved videos...");

        QueryWrapper<Video> query = new QueryWrapper<>();
        query.eq("status", "APPROVED");
        List<Video> allVideos = videoMapper.selectList(query);

        if (allVideos.isEmpty()) {
            log.warn("No approved videos found in database");
            return;
        }

        String trendingKey = RedisKeyConstants.Recommend.getTrendingKey();
        int count = 0;

        for (Video video : allVideos) {
            double score = calculateScore(video);
            redisTemplate.opsForZSet().add(trendingKey, video.getId().toString(), score);
            count++;
        }

        log.info("Initialized {} video scores in Redis", count);
    }

    private double calculateScore(Video video) {
        long playCount = video.getViewCount() == null ? 0 : video.getViewCount();
        long likeCount = video.getLikeCount() == null ? 0 : video.getLikeCount();
        long commentCount = video.getCommentCount() == null ? 0 : video.getCommentCount();

        // Base score ensures new/unpopular videos still appear
        double baseScore = 10.0;
        double interactionScore = baseScore + playCount * 0.4 + likeCount * 0.3 + commentCount * 0.2;

        long hoursSincePublish = 0;
        if (video.getPublishedAt() != null) {
            hoursSincePublish = Duration.between(video.getPublishedAt(), LocalDateTime.now()).toHours();
        }
        // Avoid negative hours if clock skew
        if (hoursSincePublish < 0)
            hoursSincePublish = 0;

        // Gentler time decay factor (changed from -0.1 to -0.01)
        double timeDecay = Math.exp(-0.01 * hoursSincePublish);

        return interactionScore * timeDecay;
    }

    @Override
    public Result<List<com.flowbrain.viewx.pojo.vo.VideoListVO>> searchVideos(String keyword, Long userId, int page,
            int size) {
        try {
            // 使用 MyBatis-Plus 的 QueryWrapper 进行搜索
            QueryWrapper<Video> query = new QueryWrapper<>();
            query.eq("is_deleted", false)
                    .eq("status", "APPROVED")
                    .and(wrapper -> wrapper
                            .like("title", keyword)
                            .or()
                            .like("description", keyword))
                    .orderByDesc("created_at")
                    .last("LIMIT " + size + " OFFSET " + ((page - 1) * size));

            List<Video> videos = videoMapper.selectList(query);

            // 转换为 VO 并填充额外信息
            List<com.flowbrain.viewx.pojo.vo.VideoListVO> videoVOs = videos.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            log.info("搜索视频成功，关键词: {}, 结果数: {}", keyword, videoVOs.size());
            return Result.success(videoVOs);
        } catch (Exception e) {
            log.error("搜索视频失败，关键词: {}", keyword, e);
            return Result.serverError("搜索视频失败");
        }
    }
}
