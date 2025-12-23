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
 * 1. 只缓存前 5 页数据（深度分页不缓存）
 * 2. 根据数据热度设置不同的过期时间
 * 3. 使用条件缓存，避免缓存冷数据
 */
@Slf4j
@Service
public class VideoCacheService {

    private final VideoService videoService;

    public VideoCacheService(VideoService videoService) {
        this.videoService = videoService;
    }

    /**
     * 获取热门视频列表（带缓存）
     * 只缓存前 5 页，深度分页不缓存
     * 缓存时间：5分钟
     */
    @Cacheable(value = "hot-videos", 
               key = "'page:' + #page + ':size:' + #size",
               condition = "#page <= 5")
    public List<VideoListVO> getHotVideos(int page, int size) {
        log.debug("从数据库查询热门视频列表: page={}, size={}", page, size);
        return videoService.getHotVideosFromDB(page, size);
    }

    /**
     * 获取用户视频列表（带缓存）
     * 只缓存前 3 页
     * 缓存时间：3分钟
     */
    @Cacheable(value = "user-videos", 
               key = "'user:' + #userId + ':page:' + #page + ':size:' + #size",
               condition = "#page <= 3")
    public List<VideoListVO> getUserVideos(Long userId, int page, int size) {
        log.debug("从数据库查询用户视频列表: userId={}, page={}", userId, page);
        return videoService.getUserVideosFromDB(userId, page, size);
    }

    /**
     * 获取分类视频列表（带缓存）
     * 只缓存前 5 页
     * 缓存时间：5分钟
     */
    @Cacheable(value = "category-videos", 
               key = "'category:' + #category + ':page:' + #page + ':size:' + #size",
               condition = "#page <= 5")
    public List<VideoListVO> getCategoryVideos(String category, int page, int size) {
        log.debug("从数据库查询分类视频列表: category={}, page={}", category, page);
        return videoService.getCategoryVideosFromDB(category, page, size);
    }

    /**
     * 清除视频相关缓存
     * 场景：视频更新、删除时调用
     */
    @Caching(evict = {
        @CacheEvict(value = "user-videos", allEntries = true),
        @CacheEvict(value = "hot-videos", allEntries = true),
        @CacheEvict(value = "category-videos", allEntries = true)
    })
    public void evictVideoCache(Long videoId) {
        log.info("清除视频缓存: videoId={}", videoId);
    }

    /**
     * 清除用户视频列表缓存
     * 场景：用户上传新视频时调用
     */
    @CacheEvict(value = "user-videos", allEntries = true)
    public void evictUserVideosCache(Long userId) {
        log.info("清除用户视频列表缓存: userId={}", userId);
    }

    /**
     * 清除热门视频缓存
     * 场景：视频浏览量大幅变化时调用
     */
    @CacheEvict(value = "hot-videos", allEntries = true)
    public void evictHotVideosCache() {
        log.info("清除热门视频缓存");
    }

    /**
     * 清除分类视频缓存
     * 场景：视频分类变更时调用
     */
    @CacheEvict(value = "category-videos", allEntries = true)
    public void evictCategoryVideosCache(String category) {
        log.info("清除分类视频缓存: category={}", category);
    }
}
