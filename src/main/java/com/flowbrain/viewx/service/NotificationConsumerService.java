package com.flowbrain.viewx.service;

import com.flowbrain.viewx.config.RabbitMQConfig;
import com.flowbrain.viewx.dao.NotificationMapper;
import com.flowbrain.viewx.pojo.dto.NotificationMessageDTO;
import com.flowbrain.viewx.pojo.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Notification Consumer Service
 * Consumes notification messages from RabbitMQ and processes them
 */
@Slf4j
@Service
public class NotificationConsumerService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Process notification messages from RabbitMQ
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION)
    public void processNotification(NotificationMessageDTO message) {
        try {
            log.info("Processing notification: type={}, recipient={}, sender={}",
                    message.getNotificationType(), message.getRecipientId(), message.getSenderId());

            // Save notification to database
            Notification notification = new Notification();
            notification.setRecipientId(message.getRecipientId());
            notification.setSenderId(message.getSenderId());
            notification.setNotificationType(message.getNotificationType());
            notification.setRelatedVideoId(message.getVideoId());
            notification.setRelatedCommentId(message.getCommentId());
            notification.setContent(message.getContent());
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setIsDeleted(false);

            notificationMapper.insert(notification);

            // Send real-time notification via WebSocket
            sendWebSocketNotification(message.getRecipientId(), notification);

            log.info("Notification processed successfully: id={}", notification.getId());

        } catch (Exception e) {
            log.error("Failed to process notification: {}", message, e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    /**
     * Send notification to user via WebSocket
     */
    private void sendWebSocketNotification(Long userId, Notification notification) {
        try {
            String destination = "/topic/notifications/" + userId;
            messagingTemplate.convertAndSend(destination, notification);
            log.debug("Sent WebSocket notification to user: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to user {}: {}", userId, e.getMessage());
            // Don't throw exception - WebSocket delivery is best-effort
        }
    }
}
