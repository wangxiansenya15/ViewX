package com.flowbrain.viewx.pojo.dto;

import com.flowbrain.viewx.common.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Notification Message DTO for RabbitMQ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Recipient user ID
     */
    private Long recipientId;

    /**
     * Sender user ID
     */
    private Long senderId;

    /**
     * Notification type
     */
    private NotificationType notificationType;

    /**
     * Related video ID (for like, comment, favorite notifications)
     */
    private Long videoId;

    /**
     * Related comment ID (for comment reply notifications)
     */
    private Long commentId;

    /**
     * Notification content/message
     */
    private String content;

    /**
     * Additional metadata (JSON string)
     */
    private String metadata;
}
