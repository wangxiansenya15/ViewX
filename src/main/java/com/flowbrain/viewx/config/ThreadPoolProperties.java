package com.flowbrain.viewx.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "viewx.thread-pool")
public class ThreadPoolProperties {
    /**
     * 核心线程数
     * 2核2G建议：CPU密集型设为2+1，IO密集型设为2*2
     */
    private int corePoolSize = 4;
    
    /**
     * 最大线程数
     * 2G内存不宜过大，防止OOM
     */
    private int maxPoolSize = 10;
    
    /**
     * 队列容量
     * 缓冲任务，避免瞬时流量冲垮系统
     */
    private int queueCapacity = 200;
    
    /**
     * 线程存活时间(秒)
     */
    private int keepAliveSeconds = 60;
    
    /**
     * 线程名前缀
     */
    private String threadNamePrefix = "viewx-async-";
}
