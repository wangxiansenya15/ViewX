package com.flowbrain.viewx.service;

import com.flowbrain.viewx.config.RabbitMQConfig;
import com.flowbrain.viewx.dao.NotificationMapper;
import com.flowbrain.viewx.dao.VideoMapper;
import com.flowbrain.viewx.pojo.entity.Notification;
import com.flowbrain.viewx.pojo.entity.Video;
import com.flowbrain.viewx.pojo.vo.UserActionMessage;
import com.flowbrain.viewx.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationConsumer {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private VideoMapper videoMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION)
    public void handleNotification(UserActionMessage msg) {
        log.info("🔔 处理通知消息: {}", msg);

        if (msg.getVideoId() == null) return;

        // 1. 获取视频信息以确定接收者（作者）
        Video video = videoMapper.selectById(msg.getVideoId());
        if (video == null) {
            log.warn("视频不存在，无法发送通知: {}", msg.getVideoId());
            return;
        }

        Long recipientId = video.getUploaderId();
        
        // 不给自己发通知
        if (recipientId.equals(msg.getUserId())) {
            return;
        }

        // 2. 构建通知对象
        Notification notification = new Notification();
        notification.setId(IdGenerator.nextId());
        notification.setRecipientId(recipientId);
        notification.setSenderId(msg.getUserId());
        notification.setRelatedVideoId(msg.getVideoId());
        
        String type = "SYSTEM";
        String content = "";
        
        switch (msg.getActionType()) {
            case "like":
                type = "LIKE_VIDEO";
                content = "赞了你的视频";
                break;
            case "comment":
                type = "COMMENT_VIDEO";
                content = "评论了你的视频";
                break;
            case "share":
                type = "SHARE_VIDEO";
                content = "分享了你的视频";
                break;
            default:
                return; // 忽略其他类型
        }
        
        notification.setNotificationType(type);
        notification.setContent(content);
        
        // 3. 保存通知
        try {
            notificationMapper.insert(notification);
            log.info("通知已保存，接收者: {}", recipientId);
            // TODO: 这里可以进一步触发 WebSocket 推送
        } catch (Exception e) {
            log.error("保存通知失败", e);
        }
    }
}
