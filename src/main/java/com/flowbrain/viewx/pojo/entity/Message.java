package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息实体类
 */
@Data
@TableName("vx_messages")
public class Message {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private String messageType; // TEXT, IMAGE, VIDEO, EMOJI
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
