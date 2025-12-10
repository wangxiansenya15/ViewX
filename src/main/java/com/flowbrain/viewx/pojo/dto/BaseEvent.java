package com.flowbrain.viewx.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一的消息事件基类
 * 所有 MQ 消息都继承此类，便于统一处理和追踪
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 事件唯一ID（用于幂等性控制）
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件发生时间
     */
    private LocalDateTime timestamp;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 扩展数据（JSON 格式）
     */
    private Map<String, Object> data;

    /**
     * 重试次数
     */
    private Integer retryCount = 0;
}
