package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.flowbrain.viewx.common.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知实体类
 */
@Data
@TableName("vx_notifications")
public class Notification {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 接收者ID
     */
    private Long recipientId;

    /**
     * 发送者ID (触发动作的人)
     */
    private Long senderId;

    /**
     * 通知类型
     */
    private NotificationType notificationType;

    /**
     * 关联视频ID
     */
    private Long relatedVideoId;

    /**
     * 关联评论ID
     */
    private Long relatedCommentId;

    /**
     * 通知内容快照
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
     * 软删除标记
     */
    @TableLogic
    private Boolean isDeleted;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;
}
