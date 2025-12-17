package com.flowbrain.viewx.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.dao.NotificationMapper;
import com.flowbrain.viewx.pojo.dto.NotificationQueryDTO;
import com.flowbrain.viewx.pojo.entity.Notification;
import com.flowbrain.viewx.common.enums.NotificationType;
import com.flowbrain.viewx.pojo.vo.NotificationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务
 */
@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    /**
     * 创建通知 (异步)
     */
    @Async
    @Transactional
    public void createNotification(Long recipientId, Long senderId, NotificationType type,
            Long videoId, Long commentId, String content) {
        // 不给自己发送通知
        if (recipientId.equals(senderId)) {
            return;
        }

        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setSenderId(senderId);
        notification.setNotificationType(type);
        notification.setRelatedVideoId(videoId);
        notification.setRelatedCommentId(commentId);
        notification.setContent(content);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setIsDeleted(false);

        notificationMapper.insert(notification);
        log.info("Created notification: type={}, recipient={}, sender={}", type, recipientId, senderId);
    }

    /**
     * 获取通知列表
     */
    public Result<List<NotificationVO>> getNotifications(Long userId, NotificationQueryDTO queryDTO) {
        try {
            int offset = (queryDTO.getPage() - 1) * queryDTO.getPageSize();

            List<NotificationVO> notifications = notificationMapper.selectNotificationList(
                    userId,
                    queryDTO.getNotificationType() != null ? queryDTO.getNotificationType().name() : null,
                    queryDTO.getUnreadOnly(),
                    queryDTO.getPageSize(),
                    offset);

            // 添加时间描述
            notifications.forEach(n -> {
                n.setNotificationTypeDesc(n.getNotificationType().getDescription());
                n.setTimeDesc(getTimeDesc(n.getCreatedAt()));
            });

            return Result.success(notifications);
        } catch (Exception e) {
            log.error("获取通知列表失败", e);
            return Result.serverError("获取通知列表失败");
        }
    }

    /**
     * 获取未读通知数量
     */
    public Result<Long> getUnreadCount(Long userId) {
        try {
            Long count = notificationMapper.countUnread(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取未读数量失败", e);
            return Result.serverError("获取未读数量失败");
        }
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public Result<String> markAsRead(Long userId, Long notificationId) {
        try {
            LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Notification::getId, notificationId)
                    .eq(Notification::getRecipientId, userId)
                    .set(Notification::getIsRead, true);

            int updated = notificationMapper.update(null, wrapper);
            if (updated > 0) {
                return Result.success("标记成功");
            } else {
                return Result.notFound("通知不存在");
            }
        } catch (Exception e) {
            log.error("标记失败", e);
            return Result.serverError("标记失败");
        }
    }

    /**
     * 标记所有通知为已读
     */
    @Transactional
    public Result<String> markAllAsRead(Long userId) {
        try {
            int updated = notificationMapper.markAllAsRead(userId);
            return Result.success("已标记 " + updated + " 条通知为已读");
        } catch (Exception e) {
            log.error("标记失败", e);
            return Result.serverError("标记失败");
        }
    }

    /**
     * 删除通知
     */
    @Transactional
    public Result<String> deleteNotification(Long userId, Long notificationId) {
        try {
            LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Notification::getId, notificationId)
                    .eq(Notification::getRecipientId, userId);

            Notification notification = notificationMapper.selectOne(queryWrapper);
            if (notification == null) {
                return Result.notFound("通知不存在");
            }

            // 软删除
            notification.setIsDeleted(true);
            notification.setDeletedAt(LocalDateTime.now());
            notificationMapper.updateById(notification);

            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除失败", e);
            return Result.serverError("删除失败");
        }
    }

    /**
     * 生成时间描述 (如: 3分钟前)
     */
    private String getTimeDesc(LocalDateTime createdAt) {
        Duration duration = Duration.between(createdAt, LocalDateTime.now());

        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return "刚刚";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟前";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时前";
        } else if (seconds < 2592000) {
            return (seconds / 86400) + "天前";
        } else {
            return createdAt.toLocalDate().toString();
        }
    }
}
