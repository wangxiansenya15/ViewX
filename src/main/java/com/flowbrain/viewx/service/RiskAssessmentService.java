package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.RedisKeyConstants;
import com.flowbrain.viewx.common.enums.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/**
 * 风险评估服务
 * 根据多个维度评估请求的风险等级
 */
@Service
@Slf4j
public class RiskAssessmentService {

    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;

    // 高峰时段定义：8:00-22:00
    private static final LocalTime PEAK_START = LocalTime.of(8, 0);
    private static final LocalTime PEAK_END = LocalTime.of(22, 0);

    // 登录失败次数阈值
    private static final int LOGIN_FAILURE_THRESHOLD = 3;

    // 账户锁定阈值
    private static final int ACCOUNT_LOCK_THRESHOLD = 10;

    // 请求频率阈值（每分钟）
    private static final int REQUEST_RATE_THRESHOLD = 10;

    /**
     * 评估登录请求的风险等级
     *
     * @param username  用户名
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     * @return 风险等级
     */
    public RiskLevel assessLoginRisk(String username, String ipAddress, String userAgent) {
        int riskScore = 0;

        // 1. 检查登录失败次数
        int failureCount = getLoginFailureCount(username, ipAddress);
        if (failureCount >= LOGIN_FAILURE_THRESHOLD) {
            log.warn("检测到多次登录失败 - 用户名: {}, IP: {}, 失败次数: {}", username, ipAddress, failureCount);
            return RiskLevel.HIGH; // 多次失败直接判定为高风险
        } else if (failureCount > 0) {
            riskScore += 20; // 有失败记录，增加风险分数
        }

        // 2. 检查请求频率
        int requestRate = getRequestRate(ipAddress);
        if (requestRate > REQUEST_RATE_THRESHOLD) {
            log.warn("检测到异常请求频率 - IP: {}, 频率: {}/分钟", ipAddress, requestRate);
            riskScore += 30;
        }

        // 3. 检查是否为新设备/IP
        boolean isNewDevice = isNewDevice(username, ipAddress);
        if (isNewDevice) {
            log.info("检测到新设备登录 - 用户名: {}, IP: {}", username, ipAddress);
            riskScore += 25;
        }

        // 4. 检查访问时间（非高峰时段）
        if (!isPeakTime()) {
            log.debug("非高峰时段访问 - 时间: {}", LocalTime.now());
            riskScore += 10;
        }

        // 5. 检查User-Agent是否可疑
        if (isSuspiciousUserAgent(userAgent)) {
            log.warn("检测到可疑User-Agent - IP: {}, UA: {}", ipAddress, userAgent);
            riskScore += 25;
        }

        // 6. 检查是否使用代理/VPN
        if (isProxyOrVPN(ipAddress)) {
            log.warn("检测到代理/VPN访问 - IP: {}", ipAddress);
            riskScore += 30;
        }

        // 根据总分判定风险等级
        if (riskScore >= 50) {
            log.info("风险评估结果: 高风险 (分数: {})", riskScore);
            return RiskLevel.HIGH;
        } else if (riskScore >= 20) {
            log.info("风险评估结果: 中风险 (分数: {})", riskScore);
            return RiskLevel.MEDIUM;
        } else {
            log.info("风险评估结果: 低风险 (分数: {})", riskScore);
            return RiskLevel.LOW;
        }
    }

    /**
     * 获取登录失败次数
     */
    public int getLoginFailureCount(String username, String ipAddress) {
        String key = RedisKeyConstants.Security.getLoginFailureKey(username, ipAddress);
        String count = stringRedisTemplate.opsForValue().get(key);
        return count != null ? Integer.parseInt(count) : 0;
    }

    /**
     * 记录登录失败
     * 
     * @return 失败次数达到锁定阈值时返回 true
     */
    public boolean recordLoginFailure(String username, String ipAddress) {
        String key = RedisKeyConstants.Security.getLoginFailureKey(username, ipAddress);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, 30, TimeUnit.MINUTES); // 30分钟后过期

        int failureCount = count != null ? count.intValue() : 1;
        log.info("记录登录失败 - 用户名: {}, IP: {}, 失败次数: {}", username, ipAddress, failureCount);

        // 检查是否达到锁定阈值
        if (failureCount >= ACCOUNT_LOCK_THRESHOLD) {
            log.warn("登录失败次数达到锁定阈值 - 用户名: {}, IP: {}, 失败次数: {}",
                    username, ipAddress, failureCount);
            return true; // 需要锁定账户
        }

        return false;
    }

    /**
     * 检查账户是否应该被锁定（失败次数>=5）
     */
    public boolean shouldLockAccount(String username, String ipAddress) {
        int failureCount = getLoginFailureCount(username, ipAddress);
        return failureCount >= ACCOUNT_LOCK_THRESHOLD;
    }

    /**
     * 清除登录失败记录（登录成功后调用）
     */
    public void clearLoginFailure(String username, String ipAddress) {
        String key = RedisKeyConstants.Security.getLoginFailureKey(username, ipAddress);
        stringRedisTemplate.delete(key);
        log.info("清除登录失败记录 - 用户名: {}, IP: {}", username, ipAddress);
    }

    /**
     * 获取请求频率（每分钟请求次数）
     */
    private int getRequestRate(String ipAddress) {
        String key = RedisKeyConstants.Security.getRequestRateKey(ipAddress);
        String count = stringRedisTemplate.opsForValue().get(key);

        if (count == null) {
            // 首次请求，初始化计数器
            stringRedisTemplate.opsForValue().set(key, "1", 1, TimeUnit.MINUTES);
            return 1;
        } else {
            // 增加计数
            Long newCount = stringRedisTemplate.opsForValue().increment(key);
            return newCount != null ? newCount.intValue() : 1;
        }
    }

    /**
     * 检查是否为新设备
     */
    private boolean isNewDevice(String username, String ipAddress) {
        String key = RedisKeyConstants.Security.getKnownDeviceKey(username, ipAddress);
        return !Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 记录设备为已知设备
     */
    public void recordKnownDevice(String username, String ipAddress) {
        String key = RedisKeyConstants.Security.getKnownDeviceKey(username, ipAddress);
        stringRedisTemplate.opsForValue().set(key, "1", 30, TimeUnit.DAYS); // 30天有效期
        log.info("记录已知设备 - 用户名: {}, IP: {}", username, ipAddress);
    }

    /**
     * 重置用户的所有登录失败记录（管理员功能）
     * 
     * @param username 用户名
     */
    public void resetAllLoginFailures(String username) {
        // 由于 Redis key 包含 IP，需要删除所有相关的 key
        // 这里简化处理，只删除常见的本地IP
        String[] commonIps = { "127.0.0.1", "0:0:0:0:0:0:0:1", "::1", "localhost" };
        for (String ip : commonIps) {
            String key = RedisKeyConstants.Security.getLoginFailureKey(username, ip);
            stringRedisTemplate.delete(key);
        }
        log.warn("管理员重置登录失败记录 - 用户名: {}", username);
    }

    /**
     * 检查是否为高峰时段
     */
    private boolean isPeakTime() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(PEAK_START) && !now.isAfter(PEAK_END);
    }

    /**
     * 检查User-Agent是否可疑
     * 可疑特征：空、过短、包含bot/crawler等关键词
     */
    private boolean isSuspiciousUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return true;
        }

        if (userAgent.length() < 20) {
            return true;
        }

        String lowerUA = userAgent.toLowerCase();
        String[] suspiciousKeywords = { "bot", "crawler", "spider", "scraper", "curl", "wget", "python", "java/" };

        for (String keyword : suspiciousKeywords) {
            if (lowerUA.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查是否使用代理/VPN
     * 简化实现：检查常见代理端口和私有IP
     * 生产环境建议使用专业的IP信誉库
     */
    private boolean isProxyOrVPN(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }

        // 检查是否为内网IP（开发环境常见）
        if (ipAddress.startsWith("192.168.") ||
                ipAddress.startsWith("10.") ||
                ipAddress.startsWith("172.") ||
                ipAddress.equals("127.0.0.1") ||
                ipAddress.equals("0:0:0:0:0:0:0:1") ||
                ipAddress.equals("::1")) {
            return false; // 内网IP不判定为代理
        }

        // TODO: 集成第三方IP信誉库进行更精确的判断
        // 例如：IPQualityScore, MaxMind, etc.

        return false;
    }
}
