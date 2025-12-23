package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.enums.RiskLevel;
import com.flowbrain.viewx.dao.LoginAuditMapper;
import com.flowbrain.viewx.dao.SecurityEventMapper;
import com.flowbrain.viewx.pojo.entity.LoginAudit;
import com.flowbrain.viewx.pojo.entity.SecurityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 安全审计服务
 * 负责记录所有安全相关事件到数据库
 */
@Service
@Slf4j
public class SecurityAuditService {

    @Autowired
    private LoginAuditMapper loginAuditMapper;

    @Autowired
    private SecurityEventMapper securityEventMapper;

    /**
     * 记录登录尝试（异步）
     */
    @Async
    public void recordLoginAttempt(Long userId, String username, boolean success,
            String failureReason, String ipAddress, String userAgent,
            RiskLevel riskLevel, boolean captchaRequired, Boolean captchaVerified) {
        try {
            LoginAudit audit = new LoginAudit();
            // ID由数据库自动生成（BIGSERIAL）
            audit.setUserId(userId);
            audit.setUsername(username);
            audit.setSuccess(success);
            audit.setFailureReason(failureReason);
            audit.setIpAddress(ipAddress);
            audit.setUserAgent(userAgent);
            audit.setRiskLevel(riskLevel);
            audit.setCaptchaRequired(captchaRequired);
            audit.setCaptchaVerified(captchaVerified);
            audit.setLoginTime(LocalDateTime.now());

            loginAuditMapper.insert(audit);
            log.debug("登录审计记录已保存 - 用户名: {}, 成功: {}", username, success);
        } catch (Exception e) {
            log.error("保存登录审计记录失败", e);
        }
    }

    /**
     * 记录安全事件（异步）
     */
    @Async
    public void recordSecurityEvent(String eventType, String severity,
            String username, String ipAddress, String description) {
        try {
            SecurityEvent event = new SecurityEvent();
            // ID由数据库自动生成（BIGSERIAL）
            event.setEventType(eventType);
            event.setSeverity(severity);
            event.setUsername(username);
            event.setIpAddress(ipAddress);
            event.setDescription(description);
            event.setEventTime(LocalDateTime.now());
            event.setHandled(false);

            securityEventMapper.insert(event);
            log.info("安全事件已记录 - 类型: {}, 严重程度: {}, 描述: {}",
                    eventType, severity, description);
        } catch (Exception e) {
            log.error("保存安全事件失败", e);
        }
    }

    /**
     * 记录多次登录失败事件
     */
    public void recordMultipleLoginFailures(String username, String ipAddress,
            int failureCount) {
        String severity = failureCount >= 5 ? "HIGH" : "MEDIUM";
        String description = String.format("检测到多次登录失败 - 用户名: %s, 失败次数: %d",
                username, failureCount);

        recordSecurityEvent("MULTIPLE_LOGIN_FAILURES", severity, username, ipAddress, description);
    }

    /**
     * 记录可疑User-Agent事件
     */
    public void recordSuspiciousUserAgent(String username, String ipAddress,
            String userAgent) {
        String description = String.format("检测到可疑User-Agent - 用户名: %s, UA: %s",
                username, userAgent);

        recordSecurityEvent("SUSPICIOUS_USER_AGENT", "MEDIUM", username, ipAddress, description);
    }

    /**
     * 记录异常请求频率事件
     */
    public void recordAbnormalRequestRate(String ipAddress, int requestCount) {
        String severity = requestCount > 20 ? "HIGH" : "MEDIUM";
        String description = String.format("检测到异常请求频率 - IP: %s, 频率: %d/分钟",
                ipAddress, requestCount);

        recordSecurityEvent("ABNORMAL_REQUEST_RATE", severity, null, ipAddress, description);
    }

    /**
     * 记录人机验证失败事件
     */
    public void recordCaptchaFailure(String username, String ipAddress,
            String captchaType, RiskLevel riskLevel) {
        String description = String.format("人机验证失败 - 用户名: %s, 类型: %s, 风险等级: %s",
                username, captchaType, riskLevel.getLabel());

        recordSecurityEvent("CAPTCHA_FAILURE", "MEDIUM", username, ipAddress, description);
    }
}
