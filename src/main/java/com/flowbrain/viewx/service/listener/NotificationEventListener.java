package com.flowbrain.viewx.service.listener;

import com.flowbrain.viewx.common.EventType;
import com.flowbrain.viewx.config.RabbitMQConfig;
import com.flowbrain.viewx.dao.VideoMapper;
import com.flowbrain.viewx.pojo.dto.BaseEvent;
import com.flowbrain.viewx.pojo.entity.Video;
import com.flowbrain.viewx.common.enums.NotificationType;
import com.flowbrain.viewx.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 通知事件监听器
 * 监听 MQ 中的事件并创建相应的通知
 */
@Slf4j
@Component
public class NotificationEventListener {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private VideoMapper videoMapper;

    /**
     * 监听通知队列
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION)
    public void handleNotificationEvent(BaseEvent event) {
        try {
            log.info("收到通知事件: eventType={}, userId={}", event.getEventType(), event.getUserId());

            String eventType = event.getEventType();
            Long senderId = event.getUserId();
            Map<String, Object> data = event.getData();

            // 根据事件类型创建不同的通知
            switch (eventType) {
                case EventType.VIDEO_LIKE:
                    handleVideoLike(senderId, data);
                    break;

                case EventType.VIDEO_FAVORITE:
                    handleVideoFavorite(senderId, data);
                    break;

                case EventType.COMMENT_CREATE:
                    handleCommentCreate(senderId, data);
                    break;

                case EventType.USER_FOLLOW:
                    handleUserFollow(senderId, data);
                    break;

                case EventType.VIDEO_APPROVED:
                    handleVideoApproved(senderId, data);
                    break;

                case EventType.VIDEO_REJECTED:
                    handleVideoRejected(senderId, data);
                    break;

                default:
                    log.warn("未知的通知事件类型: {}", eventType);
            }
        } catch (Exception e) {
            log.error("处理通知事件失败", e);
        }
    }

    /**
     * 处理视频点赞事件
     */
    private void handleVideoLike(Long senderId, Map<String, Object> data) {
        Long videoId = getLong(data, "videoId");
        if (videoId == null)
            return;

        // 获取视频信息,找到视频作者
        Video video = videoMapper.selectById(videoId);
        if (video == null)
            return;

        Long recipientId = video.getUploaderId();

        // 创建通知
        notificationService.createNotification(
                recipientId,
                senderId,
                NotificationType.LIKE_VIDEO,
                videoId,
                null,
                null);
    }

    /**
     * 处理视频收藏事件
     */
    private void handleVideoFavorite(Long senderId, Map<String, Object> data) {
        Long videoId = getLong(data, "videoId");
        if (videoId == null)
            return;

        Video video = videoMapper.selectById(videoId);
        if (video == null)
            return;

        Long recipientId = video.getUploaderId();

        notificationService.createNotification(
                recipientId,
                senderId,
                NotificationType.FAVORITE_VIDEO,
                videoId,
                null,
                null);
    }

    /**
     * 处理评论创建事件
     */
    private void handleCommentCreate(Long senderId, Map<String, Object> data) {
        Long videoId = getLong(data, "videoId");
        Long commentId = getLong(data, "commentId");
        String content = (String) data.get("content");

        if (videoId == null || commentId == null)
            return;

        Video video = videoMapper.selectById(videoId);
        if (video == null)
            return;

        Long recipientId = video.getUploaderId();

        notificationService.createNotification(
                recipientId,
                senderId,
                NotificationType.COMMENT_VIDEO,
                videoId,
                commentId,
                content);
    }

    /**
     * 处理用户关注事件
     */
    private void handleUserFollow(Long senderId, Map<String, Object> data) {
        Long followedId = getLong(data, "followedId");
        if (followedId == null)
            return;

        notificationService.createNotification(
                followedId,
                senderId,
                NotificationType.FOLLOW,
                null,
                null,
                null);
    }

    /**
     * 处理视频审核通过事件
     */
    private void handleVideoApproved(Long userId, Map<String, Object> data) {
        Long videoId = getLong(data, "videoId");
        if (videoId == null)
            return;

        notificationService.createNotification(
                userId,
                null, // 系统通知,没有发送者
                NotificationType.VIDEO_APPROVED,
                videoId,
                null,
                "你的视频已通过审核");
    }

    /**
     * 处理视频审核拒绝事件
     */
    private void handleVideoRejected(Long userId, Map<String, Object> data) {
        Long videoId = getLong(data, "videoId");
        String reason = (String) data.get("reason");
        if (videoId == null)
            return;

        notificationService.createNotification(
                userId,
                null, // 系统通知,没有发送者
                NotificationType.VIDEO_REJECTED,
                videoId,
                null,
                reason != null ? "审核未通过: " + reason : "你的视频审核未通过");
    }

    /**
     * 从 Map 中安全获取 Long 值
     */
    private Long getLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null)
            return null;
        if (value instanceof Long)
            return (Long) value;
        if (value instanceof Integer)
            return ((Integer) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
