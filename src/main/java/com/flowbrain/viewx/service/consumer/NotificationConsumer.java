package com.flowbrain.viewx.service.consumer;

import com.flowbrain.viewx.config.RabbitMQConfig;
import com.flowbrain.viewx.common.EventType;
import com.flowbrain.viewx.dao.NotificationMapper;
import com.flowbrain.viewx.pojo.dto.BaseEvent;
import com.flowbrain.viewx.pojo.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 通知消费者
 * 功能：处理需要发送通知的事件，创建站内通知
 */
@Service
@Slf4j
public class NotificationConsumer {

    @Autowired
    private NotificationMapper notificationMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION)
    public void handleNotification(BaseEvent event) {
        try {
            log.info("接收到通知事件: eventId={}, eventType={}", event.getEventId(), event.getEventType());

            String eventType = event.getEventType();
            Notification notification = createNotification(event);

            if (notification != null) {
                notificationMapper.insert(notification);
                log.info("通知创建成功: eventId={}, notificationType={}", event.getEventId(), notification.getType());
            }

            // Spring AMQP 自动 ACK

        } catch (Exception e) {
            log.error("通知处理失败: eventId={}", event.getEventId(), e);
            throw new RuntimeException("通知处理失败", e);
        }
    }

    /**
     * 根据事件创建通知
     */
    private Notification createNotification(BaseEvent event) {
        String eventType = event.getEventType();
        Notification notification = new Notification();
        notification.setCreatedAt(LocalDateTime.now());
        notification.setIsRead(false);

        switch (eventType) {
            case EventType.COMMENT_CREATE:
                return createCommentNotification(event, notification);
            case EventType.USER_FOLLOW:
                return createFollowNotification(event, notification);
            case EventType.VIDEO_APPROVED:
                return createVideoApprovedNotification(event, notification);
            case EventType.VIDEO_REJECTED:
                return createVideoRejectedNotification(event, notification);
            default:
                log.warn("未处理的通知事件类型: {}", eventType);
                return null;
        }
    }

    /**
     * 创建评论通知
     */
    private Notification createCommentNotification(BaseEvent event, Notification notification) {
        Long videoId = (Long) event.getData().get("videoId");
        String content = (String) event.getData().get("content");

        // TODO: 查询视频作者ID
        // notification.setUserId(videoOwnerId);
        notification.setType("COMMENT");
        notification.setTitle("新评论");
        notification.setContent("有人评论了你的视频: " + content);
        notification.setRelatedId(videoId);

        return notification;
    }

    /**
     * 创建关注通知
     */
    private Notification createFollowNotification(BaseEvent event, Notification notification) {
        Long followedId = (Long) event.getData().get("followedId");
        Long followerId = event.getUserId();

        notification.setUserId(followedId);
        notification.setType("FOLLOW");
        notification.setTitle("新粉丝");
        notification.setContent("用户 " + followerId + " 关注了你");
        notification.setRelatedId(followerId);

        return notification;
    }

    /**
     * 创建视频审核通过通知
     */
    private Notification createVideoApprovedNotification(BaseEvent event, Notification notification) {
        Long videoId = (Long) event.getData().get("videoId");

        notification.setUserId(event.getUserId());
        notification.setType("VIDEO_APPROVED");
        notification.setTitle("视频审核通过");
        notification.setContent("你的视频已通过审核，可以正常展示了");
        notification.setRelatedId(videoId);

        return notification;
    }

    /**
     * 创建视频审核拒绝通知
     */
    private Notification createVideoRejectedNotification(BaseEvent event, Notification notification) {
        Long videoId = (Long) event.getData().get("videoId");
        String reason = (String) event.getData().getOrDefault("reason", "不符合平台规范");

        notification.setUserId(event.getUserId());
        notification.setType("VIDEO_REJECTED");
        notification.setTitle("视频审核未通过");
        notification.setContent("你的视频未通过审核，原因: " + reason);
        notification.setRelatedId(videoId);

        return notification;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_DLQ)
    public void handleDeadLetter(BaseEvent event) {
        log.error("通知死信消息: eventId={}", event.getEventId());
        // Spring AMQP 自动 ACK
    }
}
