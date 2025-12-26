package com.flowbrain.viewx.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Cache 配置
 * 
 * 针对低内存服务器优化：
 * 1. 只缓存热门数据（前3页）
 * 2. 设置合理的TTL，自动过期释放内存
 * 3. 使用Redis作为缓存存储，不占用JVM堆内存
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置缓存管理器
     * 为不同的缓存设置不同的过期时间
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置：5分钟过期
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues(); // 不缓存null值，节省内存

        // 为不同的缓存设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 热门视频缓存：5分钟（访问频繁，更新较慢）
        cacheConfigurations.put("trending-videos",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // 推荐视频缓存：3分钟（个性化推荐，更新较快）
        cacheConfigurations.put("recommended-videos",
                defaultConfig.entryTtl(Duration.ofMinutes(3)));

        // 用户信息缓存：10分钟（变化不频繁）
        cacheConfigurations.put("user-info",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // 视频详情缓存：15分钟（变化很少）
        cacheConfigurations.put("video-detail",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware() // 支持事务
                .build();
    }
}
