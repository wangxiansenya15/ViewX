package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class TokenService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;

    @Value("${jwt.expire}")
    private long tokenExpire;

    /**
     * 存储Token与用户关联
     */
    public void storeToken(String username, String token) {
        String key = RedisKeyConstants.Token.getUserTokenKey(username);
        stringRedisTemplate.opsForValue().set(key, token, Duration.ofSeconds(tokenExpire));
        log.info("Token存储成功，用户名: {}, 过期时间: {}秒", username, tokenExpire);
    }

    /**
     * 获取用户的Token
     */
    public String getToken(String username) {
        String key = RedisKeyConstants.Token.getUserTokenKey(username);
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 验证Token有效性
     */
    public boolean validateToken(String username, String token) {
        String storedToken = getToken(username);
        if (storedToken == null) {
            log.warn("Token不存在，用户名: {}", username);
            return false;
        }

        // 检查是否在黑名单中
        if (isTokenBlacklisted(token)) {
            log.warn("Token已被加入黑名单，用户名: {}", username);
            return false;
        }

        boolean isValid = storedToken.equals(token);
        if (!isValid) {
            log.warn("Token不匹配，用户名: {}", username);
        }
        return isValid;
    }

    /**
     * 刷新Token过期时间
     */
    public void refreshToken(String username) {
        String key = RedisKeyConstants.Token.getUserTokenKey(username);
        stringRedisTemplate.expire(key, Duration.ofSeconds(tokenExpire));
        log.debug("Token过期时间已刷新，用户名: {}", username);
    }

    /**
     * 删除Token（登出时使用）
     */
    public void deleteToken(String username) {
        String key = RedisKeyConstants.Token.getUserTokenKey(username);
        stringRedisTemplate.delete(key);
        log.info("Token已删除，用户名: {}", username);
    }

    /**
     * 将Token加入黑名单（主动失效）
     */
    public void addToBlacklist(String token, long expireSeconds) {
        String key = RedisKeyConstants.Token.getBlacklistKey(token);
        stringRedisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(expireSeconds));
        log.info("Token已加入黑名单，剩余时间: {}秒", expireSeconds);
    }

    /**
     * 检查Token是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        String key = RedisKeyConstants.Token.getBlacklistKey(token);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 获取用户在线状态
     */
    public boolean isUserOnline(String username) {
        String key = RedisKeyConstants.Token.getUserTokenKey(username);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 记录用户在线状态（扩展功能）
     */
    public void markUserOnline(String username) {
        String key = RedisKeyConstants.Token.getOnlineUserKey(username);
        stringRedisTemplate.opsForValue().set(key, "online", Duration.ofSeconds(tokenExpire));
    }

    /**
     * 清除用户在线状态
     */
    public void markUserOffline(String username) {
        String key = RedisKeyConstants.Token.getOnlineUserKey(username);
        stringRedisTemplate.delete(key);
    }
}