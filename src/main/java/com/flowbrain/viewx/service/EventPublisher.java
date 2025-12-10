package com.flowbrain.viewx.service;

import com.flowbrain.viewx.config.RabbitMQConfig;
import com.flowbrain.viewx.common.EventType;
import com.flowbrain.viewx.pojo.dto.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 统一的事件发布服务
 * 负责将各种业务事件发送到 MQ
 */
@Service
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发布事件（通用方法）
     */
    public void publishEvent(String eventType, Long userId, Map<String, Object> data) {
        BaseEvent event = new BaseEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setTimestamp(LocalDateTime.now());
        event.setUserId(userId);
        event.setData(data);

        // 根据事件类型路由到不同的队列
        routeEvent(event);
    }

    /**
     * 路由事件到对应的队列
     */
    private void routeEvent(BaseEvent event) {
        String eventType = event.getEventType();

        try {
            // 所有事件都发送到行为日志队列
            sendToQueue(RabbitMQConfig.ROUTING_KEY_LOG, event);

            // 根据事件类型发送到特定队列
            // 注意：一个事件可能需要发送到多个队列

            // 影响推荐算法的事件
            if (EventType.VIDEO_PLAY.equals(eventType) ||
                    EventType.VIDEO_LIKE.equals(eventType) ||
                    EventType.VIDEO_UNLIKE.equals(eventType) ||
                    EventType.VIDEO_FAVORITE.equals(eventType) ||
                    EventType.VIDEO_SHARE.equals(eventType)) {
                sendToQueue(RabbitMQConfig.ROUTING_KEY_RECOMMEND, event);
                sendToQueue(RabbitMQConfig.ROUTING_KEY_STATISTICS, event);
            }

            // 需要发送通知的事件
            if (EventType.COMMENT_CREATE.equals(eventType) ||
                    EventType.USER_FOLLOW.equals(eventType) ||
                    EventType.VIDEO_APPROVED.equals(eventType)) {
                sendToQueue(RabbitMQConfig.ROUTING_KEY_NOTIFICATION, event);
            }

            // 需要更新搜索索引的事件
            if (EventType.VIDEO_UPLOAD.equals(eventType) ||
                    EventType.VIDEO_APPROVED.equals(eventType) ||
                    EventType.VIDEO_DELETE.equals(eventType)) {
                sendToQueue(RabbitMQConfig.ROUTING_KEY_SEARCH, event);
            }

            // 需要发送邮件的事件
            if (EventType.USER_REGISTER.equals(eventType) ||
                    EventType.VIDEO_APPROVED.equals(eventType) ||
                    EventType.VIDEO_REJECTED.equals(eventType)) {
                sendToQueue(RabbitMQConfig.ROUTING_KEY_EMAIL, event);
            }

            // 需要视频处理的事件
            if (EventType.VIDEO_UPLOAD.equals(eventType)) {
                sendToQueue(RabbitMQConfig.ROUTING_KEY_VIDEO_PROCESS, event);
            }

            log.info("事件发布成功: eventId={}, eventType={}, userId={}",
                    event.getEventId(), event.getEventType(), event.getUserId());

        } catch (Exception e) {
            log.error("事件发布失败: eventId={}, eventType={}",
                    event.getEventId(), event.getEventType(), e);
        }
    }

    /**
     * 发送消息到指定队列
     */
    private void sendToQueue(String routingKey, BaseEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                routingKey,
                event);
    }

    // ==================== 便捷方法 ====================

    /**
     * 发布视频播放事件
     */
    public void publishVideoPlayEvent(Long userId, Long videoId, Integer watchDuration) {
        Map<String, Object> data = new HashMap<>();
        data.put("videoId", videoId);
        data.put("watchDuration", watchDuration);
        publishEvent(EventType.VIDEO_PLAY, userId, data);
    }

    /**
     * 发布点赞事件
     */
    public void publishLikeEvent(Long userId, Long videoId, boolean isLike) {
        Map<String, Object> data = new HashMap<>();
        data.put("videoId", videoId);
        publishEvent(isLike ? EventType.VIDEO_LIKE : EventType.VIDEO_UNLIKE, userId, data);
    }

    /**
     * 发布收藏事件
     */
    public void publishFavoriteEvent(Long userId, Long videoId) {
        Map<String, Object> data = new HashMap<>();
        data.put("videoId", videoId);
        publishEvent(EventType.VIDEO_FAVORITE, userId, data);
    }

    /**
     * 发布评论事件
     */
    public void publishCommentEvent(Long userId, Long videoId, Long commentId, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("videoId", videoId);
        data.put("commentId", commentId);
        data.put("content", content);
        publishEvent(EventType.COMMENT_CREATE, userId, data);
    }

    /**
     * 发布关注事件
     */
    public void publishFollowEvent(Long followerId, Long followedId, boolean isFollow) {
        Map<String, Object> data = new HashMap<>();
        data.put("followedId", followedId);
        publishEvent(isFollow ? EventType.USER_FOLLOW : EventType.USER_UNFOLLOW, followerId, data);
    }

    /**
     * 发布视频上传事件
     */
    public void publishVideoUploadEvent(Long userId, Long videoId, String videoUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("videoId", videoId);
        data.put("videoUrl", videoUrl);
        publishEvent(EventType.VIDEO_UPLOAD, userId, data);
    }

    /**
     * 发布用户注册事件
     */
    public void publishUserRegisterEvent(Long userId, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        publishEvent(EventType.USER_REGISTER, userId, data);
    }

    /**
     * 发布延迟事件（用于定时任务）
     */
    public void publishDelayedEvent(String eventType, Long userId, Map<String, Object> data, long delayMillis) {
        BaseEvent event = new BaseEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setTimestamp(LocalDateTime.now());
        event.setUserId(userId);
        event.setData(data);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DELAY_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_DELAY,
                event,
                message -> {
                    message.getMessageProperties().setExpiration(String.valueOf(delayMillis));
                    return message;
                });

        log.info("延迟事件发布成功: eventId={}, delay={}ms", event.getEventId(), delayMillis);
    }
}
