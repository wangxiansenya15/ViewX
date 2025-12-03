package com.flowbrain.viewx.service;

import com.flowbrain.viewx.pojo.entity.Video;
import java.util.List;

public interface RecommendService {
    
    /**
     * Get recommended videos for a specific user.
     * @param userId The user ID (can be null for guest)
     * @param page Page number
     * @param size Page size
     * @return List of recommended videos
     */
    List<Video> getRecommendedVideos(Long userId, int page, int size);

    /**
     * Get trending/hot videos.
     * @param page Page number
     * @param size Page size
     * @return List of trending videos
     */
    List<Video> getTrendingVideos(int page, int size);
    
    /**
     * Update video score (async usually).
     * @param videoId The video ID
     */
    void updateVideoScore(Long videoId);
}
