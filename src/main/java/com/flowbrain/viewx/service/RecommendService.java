package com.flowbrain.viewx.service;

import com.flowbrain.viewx.pojo.vo.VideoListVO;
import java.util.List;

import com.flowbrain.viewx.common.Result;

public interface RecommendService {

    /**
     * Get recommended videos for a specific user.
     * 
     * @param userId The user ID (can be null for guest)
     * @param page   Page number
     * @param size   Page size
     * @return List of recommended videos
     */
    List<VideoListVO> getRecommendedVideos(Long userId, int page, int size);

    /**
     * Get trending/hot videos.
     * 
     * @param page Page number
     * @param size Page size
     * @return List of trending videos
     */
    List<VideoListVO> getTrendingVideos(int page, int size);

    /**
     * Update video score (async usually).
     * 
     * @param videoId The video ID
     */
    void updateVideoScore(Long videoId);

    /**
     * 搜索视频
     * 
     * @param keyword 关键词
     * @param userId  当前用户ID（用于判断关注、点赞状态）
     * @param page    页码
     * @param size    大小
     * @return 结果
     */
    Result<List<VideoListVO>> searchVideos(String keyword, Long userId, int page, int size);
}
