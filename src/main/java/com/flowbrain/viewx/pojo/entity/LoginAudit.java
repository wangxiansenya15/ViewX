package com.flowbrain.viewx.pojo.entity;

import com.flowbrain.viewx.common.enums.RiskLevel;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录审计记录
 * 用于安全分析和行为追踪
 */
@Data
public class LoginAudit {

    private Long id;

    // 用户信息
    private Long userId;
    private String username;

    // 登录结果
    private Boolean success;
    private String failureReason;

    // 客户端信息
    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;

    // 风险评估
    private RiskLevel riskLevel;
    private Integer riskScore;

    // 人机验证
    private Boolean captchaRequired;
    private Boolean captchaVerified;
    private String captchaType;

    // 地理位置（可选）
    private String country;
    private String city;

    // 时间戳
    private LocalDateTime loginTime;
    private LocalDateTime createdAt;

    // 额外信息
    private String remarks;
}
