package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话实体类
 */
@Data
@TableName("vx_conversations")
public class Conversation {
    private Long id;
    private Long user1Id;
    private Long user2Id;
    private Long lastMessageId;
    private LocalDateTime lastMessageTime;
    private Integer unreadCountUser1;
    private Integer unreadCountUser2;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
