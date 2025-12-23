package com.flowbrain.viewx.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 用户名检查服务（带缓存优化）
 * 
 * 优化策略：
 * 1. 使用 Spring Cache 缓存查询结果
 * 2. 对于已存在的用户名，缓存时间较长（1小时）
 * 3. 对于不存在的用户名，缓存时间较短（5分钟），因为可能很快被注册
 * 4. 使用 Redis 作为缓存后端，支持分布式部署
 */
@Slf4j
@Service
public class UsernameCheckService {

    private final UserService userService;

    public UsernameCheckService(UserService userService) {
        this.userService = userService;
    }

    /**
     * 检查用户名是否存在（带缓存）
     * 
     * 缓存策略：
     * - 缓存名称：username-check
     * - 缓存键：username
     * - 缓存时间：在 application.yml 中配置
     * 
     * @param username 用户名
     * @return true 表示存在，false 表示不存在
     */
    @Cacheable(value = "username-check", key = "#username", unless = "#result == false")
    public boolean checkUsernameExists(String username) {
        log.debug("查询数据库检查用户名是否存在: {}", username);
        return userService.existsByUsername(username);
    }

    /**
     * 检查用户名是否可用
     * 
     * @param username 用户名
     * @return true 表示可用（不存在），false 表示不可用（已存在）
     */
    public boolean isUsernameAvailable(String username) {
        return !checkUsernameExists(username);
    }
}
