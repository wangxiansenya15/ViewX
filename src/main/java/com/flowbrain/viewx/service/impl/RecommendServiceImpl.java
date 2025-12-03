package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.dao.UserMapper;
import com.flowbrain.viewx.dao.VideoMapper;
import com.flowbrain.viewx.pojo.entity.Video;
import com.flowbrain.viewx.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TRENDING_KEY = "viewx:video:trending";

    @Override
    public List<Video> getRecommendedVideos(Long userId, int page, int size) {
        // 1. If user is guest or no preferences, return trending
        if (userId == null) {
            return getTrendingVideos(page, size);
        }

        // TODO: Implement personalized recommendation based on user interest tags
        // For now, return trending videos as fallback
        return getTrendingVideos(page, size);
    }

    @Override
    public List<Video> getTrendingVideos(int page, int size) {
        int start = (page - 1) * size;
        int end = start + size - 1;

        // Fetch top videos from Redis ZSet
        Set<Object> videoIds = redisTemplate.opsForZSet().reverseRange(TRENDING_KEY, start, end);
        
        if (videoIds == null || videoIds.isEmpty()) {
            // Fallback to DB if Redis is empty
            log.info("Redis trending empty, fetching from DB");
            return videoMapper.selectLatestVideos(size * (page - 1), size);
        }

        List<Long> ids = videoIds.stream()
                .map(id -> Long.parseLong(id.toString()))
                .collect(Collectors.toList());
        
        List<Video> videos = new ArrayList<>();
        for(Long id : ids) {
             Video v = videoMapper.selectById(id);
             if(v != null) videos.add(v);
        }
        return videos;
    }

    @Override
    public void updateVideoScore(Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) return;

        double score = calculateScore(video);
        
        redisTemplate.opsForZSet().add(TRENDING_KEY, videoId.toString(), score);
        log.debug("Updated score for video {}: {}", videoId, score);
    }

    private double calculateScore(Video video) {
        // Algorithm from README:
        // score = (playCount * 0.4 + likeCount * 0.3 + commentCount * 0.2) * exp(-0.1 * hoursSincePublish);
        
        long playCount = video.getViewCount() == null ? 0 : video.getViewCount();
        long likeCount = video.getLikeCount() == null ? 0 : video.getLikeCount();
        long commentCount = video.getCommentCount() == null ? 0 : video.getCommentCount();
        
        double interactionScore = playCount * 0.4 + likeCount * 0.3 + commentCount * 0.2;
        
        long hoursSincePublish = 0;
        if (video.getPublishedAt() != null) {
            hoursSincePublish = Duration.between(video.getPublishedAt(), LocalDateTime.now()).toHours();
        }
        // Avoid negative hours if clock skew
        if (hoursSincePublish < 0) hoursSincePublish = 0;
        
        // Time decay factor
        double timeDecay = Math.exp(-0.1 * hoursSincePublish);
        
        return interactionScore * timeDecay;
    }
}
