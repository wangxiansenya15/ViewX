package com.flowbrain.viewx.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * FFmpeg性能配置
 * 
 * 用于限制FFmpeg的资源使用，防止占用过多内存和CPU
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ffmpeg.performance")
public class FFmpegPerformanceConfig {

    /**
     * 最大并发FFmpeg任务数
     * 默认: 1 (低内存环境，同时只处理一个视频)
     */
    private int maxConcurrentTasks = 1;

    /**
     * FFmpeg线程数限制
     * 默认: 2 (限制FFmpeg使用的CPU核心数)
     */
    private int threads = 2;

    /**
     * 是否启用低内存模式
     * 默认: true
     */
    private boolean lowMemoryMode = true;

    /**
     * 视频处理超时时间（秒）
     * 默认: 300秒 (5分钟)
     */
    private int timeoutSeconds = 300;

    /**
     * 最大视频分辨率（宽度）
     * 超过此分辨率的视频将被压缩
     * 默认: 1280 (720p)
     */
    private int maxResolutionWidth = 1280;

    /**
     * 最大视频分辨率（高度）
     * 默认: 720
     */
    private int maxResolutionHeight = 720;

    /**
     * 视频码率限制（kbps）
     * 默认: 2000 (2Mbps)
     */
    private int maxBitrate = 2000;
}
