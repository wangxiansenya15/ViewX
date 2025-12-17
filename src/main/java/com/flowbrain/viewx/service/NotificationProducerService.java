package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.enums.NotificationType;
import com.flowbrain.viewx.config.RabbitMQConfig;
import com.flowbrain.viewx.pojo.dto.NotificationMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Notification Producer Service
 * Sends notification messages to RabbitMQ for asynchronous processing
 */
@Slf4j
@Service
public class NotificationProducerService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Send like notification
     */
    public void sendLikeNotification(Long recipientId, Long senderId, Long videoId) {
        if (recipientId.equals(senderId)) {
            return; // Don't notify yourself
        }

        NotificationMessageDTO message = NotificationMessageDTO.builder()
                .recipientId(recipientId)
                .senderId(senderId)
                .notificationType(NotificationType.LIKE_VIDEO)
                .videoId(videoId)
                .content("liked your video")
                .build();

        sendNotification(message);
        log.info("Sent like notification: recipient={}, sender={}, video={}", recipientId, senderId, videoId);
    }

    /**
     * Send comment notification
     */
    public void sendCommentNotification(Long recipientId, Long senderId, Long videoId, Long commentId,
            String commentContent) {
        if (recipientId.equals(senderId)) {
            return;
        }

        NotificationMessageDTO message = NotificationMessageDTO.builder()
                .recipientId(recipientId)
                .senderId(senderId)
                .notificationType(NotificationType.COMMENT_VIDEO)
                .videoId(videoId)
                .commentId(commentId)
                .content(commentContent)
                .build();

        sendNotification(message);
        log.info("Sent comment notification: recipient={}, sender={}, video={}", recipientId, senderId, videoId);
    }

    /**
     * Send follow notification
     */
    public void sendFollowNotification(Long recipientId, Long senderId) {
        if (recipientId.equals(senderId)) {
            return;
        }

        NotificationMessageDTO message = NotificationMessageDTO.builder()
                .recipientId(recipientId)
                .senderId(senderId)
                .notificationType(NotificationType.FOLLOW)
                .content("started following you")
                .build();

        sendNotification(message);
        log.info("Sent follow notification: recipient={}, sender={}", recipientId, senderId);
    }

    /**
     * Send favorite notification
     */
    public void sendFavoriteNotification(Long recipientId, Long senderId, Long videoId) {
        if (recipientId.equals(senderId)) {
            return;
        }

        NotificationMessageDTO message = NotificationMessageDTO.builder()
                .recipientId(recipientId)
                .senderId(senderId)
                .notificationType(NotificationType.FAVORITE_VIDEO)
                .videoId(videoId)
                .content("favorited your video")
                .build();

        sendNotification(message);
        log.info("Sent favorite notification: recipient={}, sender={}, video={}", recipientId, senderId, videoId);
    }

    /**
     * Send notification message to RabbitMQ
     */
    private void sendNotification(NotificationMessageDTO message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY_NOTIFICATION,
                    message);
        } catch (Exception e) {
            log.error("Failed to send notification to MQ: {}", message, e);
            // Fallback: could save to database directly here
        }
    }
}
