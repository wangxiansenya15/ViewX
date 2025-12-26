package com.flowbrain.viewx.service;

import com.flowbrain.viewx.pojo.vo.VideoListVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 视频缓存服务（内存优化版本）
 *
 * 优化策略：
 * 1. 只缓存前 3 页数据（深度分页不缓存）- 节省内存
 * 2. 使用条件缓存，避免缓存冷数据
 * 3. 缓存时间通过Redis TTL控制（5分钟）
 * 
 * 适用于1.6GB内存服务器的优化配置
 */
@Slf4j
@Service
public class VideoCacheService {

    private final RecommendService recommendService;

    public VideoCacheService(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    /**
     * 获取热门视频列表（带缓存）
     * 只缓存前 3 页，深度分页不缓存
     * 缓存时间：5分钟（通过Redis配置）
     */
    @Cacheable(value = "trending-videos", key = "'page:' + #page + ':size:' + #size", condition = "#page <= 3")
    public List<VideoListVO> getTrendingVideos(int page, int size) {
        log.debug("从数据库查询热门视频列表: page={}, size={}", page, size);
        return recommendService.getTrendingVideos(page, size);
    }

    /**
     * 获取推荐视频列表（带缓存）
     * 只缓存前 3 页
     * 缓存时间：3分钟
     */
    @Cacheable(value = "recommended-videos", key = "'user:' + #userId + ':page:' + #page + ':size:' + #size", condition = "#page <= 3")
    public List<VideoListVO> getRecommendedVideos(Long userId, int page, int size) {
        log.debug("从数据库查询推荐视频列表: userId={}, page={}, size={}", userId, page, size);
        return recommendService.getRecommendedVideos(userId, page, size);
    }

    /**
     * 清除所有视频缓存
     * 场景：视频更新、删除时调用
     */
    @Caching(evict = {
            @CacheEvict(value = "trending-videos", allEntries = true),
            @CacheEvict(value = "recommended-videos", allEntries = true)
    })
    public void evictAllVideoCache() {
        log.info("清除所有视频缓存");
    }

    /**
     * 清除特定视频的缓存
     * 场景：视频更新、删除时调用
     */
    @Caching(evict = {
            @CacheEvict(value = "trending-videos", allEntries = true),
            @CacheEvict(value = "recommended-videos", allEntries = true)
    })
    public void evictVideoCache(Long videoId) {
        log.info("清除视频缓存: videoId={}", videoId);
    }

    /**
     * 清除用户相关的推荐缓存
     * 场景：用户上传新视频、关注/取关时调用
     */
    @CacheEvict(value = "recommended-videos", allEntries = true)
    public void evictUserRecommendationsCache(Long userId) {
        log.info("清除用户推荐缓存: userId={}", userId);
    }

    /**
     * 清除热门视频缓存
     * 场景：视频浏览量大幅变化时调用
     */
    @CacheEvict(value = "trending-videos", allEntries = true)
    public void evictTrendingVideosCache() {
        log.info("清除热门视频缓存");
    }

    /**
     * 预热缓存 - 在系统启动或低峰期调用
     * 缓存第一页热门视频
     */
    public void warmUpCache() {
        log.info("开始预热视频缓存");
        try {
            // 预热第一页热门视频
            getTrendingVideos(1, 20);
            log.info("视频缓存预热完成");
        } catch (Exception e) {
            log.error("视频缓存预热失败", e);
        }
    }
}
