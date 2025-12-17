package com.flowbrain.viewx.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话视图对象（用于会话列表）
 */
@Data
public class ConversationVO {
    private Long conversationId;
    private Long otherUserId;
    private String otherUserUsername;
    private String otherUserNickname;
    private String otherUserAvatar;
    private Boolean isOnline;
    private String lastMessage;
    private String lastMessageType;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
}
