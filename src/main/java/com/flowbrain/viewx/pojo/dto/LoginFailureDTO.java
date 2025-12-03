package com.flowbrain.viewx.pojo.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 登录失败数据传输对象
 * 用于接收前端传入的登录失败信息
 */
@Data
public class LoginFailureDTO {
    
    // 用户名
    private String username;
    
    // 错误类型
    private String errorType;
    
    // 尝试次数
    private Integer attemptCount;
    
    // 时间戳
    private String timestamp;
    
    // 用户代理
    private String userAgent;
    
    // IP地址
    private String ipAddress;
}