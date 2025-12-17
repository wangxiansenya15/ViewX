package com.flowbrain.viewx.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息视图对象（后端返回给前端）
 */
@Data
public class MessageVO {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderNickname;
    private String senderAvatar;
    private Long receiverId;
    private String content;
    private String messageType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
