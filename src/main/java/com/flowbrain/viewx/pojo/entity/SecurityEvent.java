package com.flowbrain.viewx.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全事件记录
 * 记录各类安全相关事件，用于威胁分析
 */
@Data
public class SecurityEvent {

    private Long id;

    // 事件类型
    private String eventType; // LOGIN_FAILURE, CAPTCHA_FAILURE, RATE_LIMIT, SUSPICIOUS_UA, etc.

    // 严重程度
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    // 相关用户
    private Long userId;
    private String username;

    // 客户端信息
    private String ipAddress;
    private String userAgent;

    // 事件详情
    private String description;
    private String details; // JSON格式的详细信息

    // 处理状态
    private Boolean handled;
    private String handledBy;
    private LocalDateTime handledAt;

    // 时间戳
    private LocalDateTime eventTime;
    private LocalDateTime createdAt;
}
