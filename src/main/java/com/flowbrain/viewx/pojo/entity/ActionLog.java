package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户行为日志实体
 * 用于记录用户的各种操作行为，支持数据分析和用户画像构建
 */
@Data
@TableName("vx_action_logs")
public class ActionLog {

    /**
     * 主键ID，使用雪花算法自动生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID（可为空，支持游客行为记录）
     */
    private Long userId;

    /**
     * 行为类型（如：video.like, video.play, comment.create等）
     */
    private String actionType;

    /**
     * 关联的视频ID
     */
    private Long videoId;

    /**
     * 用户IP地址
     */
    private String ipAddress;

    /**
     * 设备信息
     */
    private String deviceInfo;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
