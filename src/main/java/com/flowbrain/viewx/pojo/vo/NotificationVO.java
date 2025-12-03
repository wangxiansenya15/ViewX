package com.flowbrain.viewx.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知 VO
 */
@Data
public class NotificationVO {
    private Long id;
    private String notificationType; // LIKE_VIDEO, COMMENT_VIDEO, REPLY_COMMENT, FOLLOW, SYSTEM
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
    
    // 发送者信息
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    
    // 关联内容
    private Long relatedVideoId;
    private String videoTitle;
    private String videoThumbnail;
    
    private Long relatedCommentId;
    private String commentContent;
}
