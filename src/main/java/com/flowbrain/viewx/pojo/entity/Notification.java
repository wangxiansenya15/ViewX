package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知实体类
 * 使用新字段名（userId, type, title等）映射到数据库的旧列名
 * 保持代码清晰的同时兼容现有数据库结构
 */
@Data
@TableName("vx_notifications")
public class Notification {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 接收通知的用户ID
     * 映射到数据库的 recipient_id 列
     */
    @TableField("recipient_id")
    private Long userId;

    /**
     * 发送者ID（触发通知的用户）
     */
    @TableField("sender_id")
    private Long senderId;

    /**
     * 通知类型
     * 映射到数据库的 notification_type 列
     * 可选值：COMMENT, FOLLOW, VIDEO_APPROVED, VIDEO_REJECTED, LIKE, REPLY 等
     */
    @TableField("notification_type")
    private String type;

    /**
     * 通知标题（新增字段，数据库中暂不存在，使用 content 的前缀）
     * 标记为不存在于数据库，通过 content 动态生成
     */
    @TableField(exist = false)
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 关联的视频ID
     */
    @TableField("related_video_id")
    private Long relatedVideoId;

    /**
     * 关联的评论ID
     */
    @TableField("related_comment_id")
    private Long relatedCommentId;

    /**
     * 通用关联ID（便捷字段，不存储到数据库）
     * 根据通知类型自动映射到 relatedVideoId 或 relatedCommentId
     */
    @TableField(exist = false)
    private Long relatedId;

    /**
     * 是否已读
     */
    @TableField("is_read")
    private Boolean isRead = false;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 软删除标记
     */
    @TableLogic
    private Boolean isDeleted = false;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    // ==================== 扩展字段（不存储到数据库） ====================

    /**
     * 发送者昵称（用于前端显示）
     */
    @TableField(exist = false)
    private String senderNickname;

    /**
     * 发送者头像（用于前端显示）
     */
    @TableField(exist = false)
    private String senderAvatar;

    /**
     * 视频标题（用于前端显示）
     */
    @TableField(exist = false)
    private String videoTitle;

    // ==================== 便捷方法 ====================

    /**
     * 设置关联ID
     * 根据通知类型自动设置到对应的字段
     */
    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;

        if (type != null) {
            if (type.contains("VIDEO") || type.contains("LIKE") || type.contains("SHARE")) {
                this.relatedVideoId = relatedId;
            } else if (type.contains("COMMENT") || type.contains("REPLY")) {
                this.relatedCommentId = relatedId;
            } else {
                // 默认设置为视频ID
                this.relatedVideoId = relatedId;
            }
        }
    }

    /**
     * 获取关联ID
     * 优先返回 relatedVideoId，其次 relatedCommentId
     */
    public Long getRelatedId() {
        if (relatedId != null) {
            return relatedId;
        }
        return relatedVideoId != null ? relatedVideoId : relatedCommentId;
    }

    /**
     * 设置标题
     * 如果没有单独的 title 字段，可以从 content 中提取或生成
     */
    public void setTitle(String title) {
        this.title = title;
        // 如果 content 为空，将 title 作为 content 的一部分
        if (this.content == null || this.content.isEmpty()) {
            this.content = title;
        }
    }

    /**
     * 获取标题
     * 如果 title 为空，从 content 中提取前50个字符作为标题
     */
    public String getTitle() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        if (content != null && !content.isEmpty()) {
            return content.length() > 50 ? content.substring(0, 50) + "..." : content;
        }
        return "";
    }
}
