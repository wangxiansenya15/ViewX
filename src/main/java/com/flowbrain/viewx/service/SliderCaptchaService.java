package com.flowbrain.viewx.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 滑块验证增强服务
 * 提供额外的安全检测，防止简单的自动化攻击
 */
@Slf4j
@Service
public class SliderCaptchaService {

    // 存储验证尝试记录 (IP -> 尝试次数)
    private final Map<String, AttemptRecord> attemptRecords = new ConcurrentHashMap<>();

    // 存储已使用的 token (防止重放攻击)
    private final Map<String, Long> usedTokens = new ConcurrentHashMap<>();

    // 配置
    private static final int MAX_ATTEMPTS_PER_MINUTE = 10; // 每分钟最多尝试次数
    private static final int MAX_ATTEMPTS_PER_HOUR = 50; // 每小时最多尝试次数
    private static final long TOKEN_VALID_DURATION = TimeUnit.MINUTES.toMillis(5); // Token 有效期 5 分钟
    private static final long CLEANUP_INTERVAL = TimeUnit.HOURS.toMillis(1); // 清理间隔 1 小时

    private long lastCleanupTime = System.currentTimeMillis();

    /**
     * 验证滑块验证
     *
     * @param token    验证 token
     * @param remoteIp 客户端 IP
     * @param request  HTTP 请求（用于额外检测）
     * @return 验证是否成功
     */
    public boolean verifySlider(String token, String remoteIp, HttpServletRequest request) {
        // 定期清理过期数据
        cleanupIfNeeded();

        // 1. 检查 token 格式
        if (token == null || !token.equals("slider-verified")) {
            log.warn("无效的滑块验证 token: {}", token);
            return false;
        }

        // 2. 检查频率限制
        if (!checkRateLimit(remoteIp)) {
            log.warn("IP {} 验证尝试过于频繁", remoteIp);
            return false;
        }

        // 3. 检查 token 是否已使用（防止重放攻击）
        if (isTokenUsed(token, remoteIp)) {
            log.warn("Token 已被使用，可能是重放攻击: {}", remoteIp);
            return false;
        }

        // 4. 额外的行为检测
        if (!checkBehavior(request)) {
            log.warn("检测到可疑行为: {}", remoteIp);
            return false;
        }

        // 5. 标记 token 为已使用
        markTokenAsUsed(token, remoteIp);

        // 6. 记录成功验证
        recordAttempt(remoteIp, true);

        log.info("滑块验证成功: {}", remoteIp);
        return true;
    }

    /**
     * 检查频率限制
     */
    private boolean checkRateLimit(String ip) {
        AttemptRecord record = attemptRecords.computeIfAbsent(ip, k -> new AttemptRecord());

        long now = System.currentTimeMillis();

        // 清理过期的尝试记录
        record.cleanupOldAttempts(now);

        // 检查每分钟限制
        long oneMinuteAgo = now - TimeUnit.MINUTES.toMillis(1);
        long recentAttempts = record.attempts.stream()
                .filter(time -> time > oneMinuteAgo)
                .count();

        if (recentAttempts >= MAX_ATTEMPTS_PER_MINUTE) {
            return false;
        }

        // 检查每小时限制
        long oneHourAgo = now - TimeUnit.HOURS.toMillis(1);
        long hourlyAttempts = record.attempts.stream()
                .filter(time -> time > oneHourAgo)
                .count();

        return hourlyAttempts < MAX_ATTEMPTS_PER_HOUR;
    }

    /**
     * 记录验证尝试
     */
    private void recordAttempt(String ip, boolean success) {
        AttemptRecord record = attemptRecords.computeIfAbsent(ip, k -> new AttemptRecord());
        record.addAttempt(System.currentTimeMillis());
        if (success) {
            record.successCount++;
        }
    }

    /**
     * 检查 token 是否已使用
     */
    private boolean isTokenUsed(String token, String ip) {
        String key = token + ":" + ip;
        Long usedTime = usedTokens.get(key);

        if (usedTime == null) {
            return false;
        }

        // 检查是否在有效期内
        long now = System.currentTimeMillis();
        return (now - usedTime) < TOKEN_VALID_DURATION;
    }

    /**
     * 标记 token 为已使用
     */
    private void markTokenAsUsed(String token, String ip) {
        String key = token + ":" + ip;
        usedTokens.put(key, System.currentTimeMillis());
    }

    /**
     * 行为检测（检测是否是真实用户）
     */
    private boolean checkBehavior(HttpServletRequest request) {
        // 1. 检查 User-Agent
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isEmpty()) {
            log.warn("缺少 User-Agent");
            return false;
        }

        // 检查是否是常见的机器人 User-Agent
        String lowerUA = userAgent.toLowerCase();
        if (lowerUA.contains("bot") ||
                lowerUA.contains("crawler") ||
                lowerUA.contains("spider") ||
                lowerUA.contains("scraper")) {
            log.warn("检测到机器人 User-Agent: {}", userAgent);
            return false;
        }

        // 2. 检查 Referer（应该来自同一域名）
        String referer = request.getHeader("Referer");
        if (referer != null) {
            String host = request.getHeader("Host");
            if (!referer.contains(host)) {
                log.warn("Referer 不匹配: {} vs {}", referer, host);
                // 不直接拒绝，只是记录警告
            }
        }

        // 3. 检查是否有必要的请求头
        String accept = request.getHeader("Accept");
        if (accept == null || accept.isEmpty()) {
            log.warn("缺少 Accept 头");
            return false;
        }

        return true;
    }

    /**
     * 定期清理过期数据
     */
    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL) {
            cleanup();
            lastCleanupTime = now;
        }
    }

    /**
     * 清理过期数据
     */
    private void cleanup() {
        long now = System.currentTimeMillis();

        // 清理过期的尝试记录
        attemptRecords.entrySet().removeIf(entry -> {
            entry.getValue().cleanupOldAttempts(now);
            return entry.getValue().attempts.isEmpty();
        });

        // 清理过期的已使用 token
        usedTokens.entrySet().removeIf(entry -> (now - entry.getValue()) > TOKEN_VALID_DURATION);

        log.info("清理完成，剩余记录: {} 个 IP, {} 个已使用 token",
                attemptRecords.size(), usedTokens.size());
    }

    /**
     * 获取 IP 的验证统计
     */
    public Map<String, Object> getStats(String ip) {
        AttemptRecord record = attemptRecords.get(ip);
        if (record == null) {
            return Map.of(
                    "attempts", 0,
                    "successCount", 0,
                    "blocked", false);
        }

        return Map.of(
                "attempts", record.attempts.size(),
                "successCount", record.successCount,
                "blocked", !checkRateLimit(ip));
    }

    /**
     * 尝试记录
     */
    private static class AttemptRecord {
        private final java.util.List<Long> attempts = new java.util.ArrayList<>();
        private int successCount = 0;

        void addAttempt(long timestamp) {
            attempts.add(timestamp);
        }

        void cleanupOldAttempts(long now) {
            long oneHourAgo = now - TimeUnit.HOURS.toMillis(1);
            attempts.removeIf(time -> time < oneHourAgo);
        }
    }
}
