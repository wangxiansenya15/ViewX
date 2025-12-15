package com.flowbrain.viewx.pojo.vo;

import com.flowbrain.viewx.common.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知视图对象
 */
@Data
public class NotificationVO {

    /**
     * 通知ID
     */
    private Long id;

    /**
     * 通知类型
     */
    private NotificationType notificationType;

    /**
     * 通知类型描述
     */
    private String notificationTypeDesc;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者用户名
     */
    private String senderUsername;

    /**
     * 发送者昵称
     */
    private String senderNickname;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 关联视频ID
     */
    private Long relatedVideoId;

    /**
     * 关联视频标题
     */
    private String relatedVideoTitle;

    /**
     * 关联视频封面
     */
    private String relatedVideoCover;

    /**
     * 关联评论ID
     */
    private Long relatedCommentId;

    /**
     * 关联评论内容
     */
    private String relatedCommentContent;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 时间描述 (如: 3分钟前)
     */
    private String timeDesc;
}
