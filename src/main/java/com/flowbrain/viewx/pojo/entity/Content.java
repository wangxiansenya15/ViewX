package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容实体 - 支持视频、图片、图片集等多种内容类型
 * 扩展原有的 Video 表,支持更丰富的内容形式
 */
@Data
@TableName("vx_contents")
public class Content {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // 内容类型: VIDEO, IMAGE, IMAGE_SET, ARTICLE 等
    private String contentType;

    private String title;
    private String description;

    // 媒体文件信息
    private String primaryUrl; // 主要媒体URL (视频URL或主图片URL)
    private String coverUrl;   // 封面图片URL
    private String thumbnailUrl;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> mediaUrls; // 多媒体URL列表 (用于图片集)

    // 视频专用字段
    private Integer duration; // 视频时长（秒），图片内容为null
    private String previewUrl;
    private Long fileSize;
    private String format;
    private String resolution;

    // 内容分类
    private String category;
    private String subcategory;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> tags;

    // 权限和状态
    private String visibility; // PUBLIC, PRIVATE, UNLISTED
    private String status;     // PENDING, APPROVED, REJECTED, DELETED, PROCESSING

    // 上传者信息
    private Long uploaderId;

    // 统计信息
    @TableField(value = "view_count")
    private Long viewCount = 0L;

    @TableField(value = "like_count")
    private Long likeCount = 0L;

    @TableField(value = "dislike_count")
    private Long dislikeCount = 0L;

    @TableField(value = "share_count")
    private Long shareCount = 0L;

    @TableField(value = "comment_count")
    private Long commentCount = 0L;

    // AI fields
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String aiTags;

    private String contentSummary;
    private Double sentimentScore;

    // 时间戳
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    // 软删除
    @TableLogic
    private Boolean isDeleted = false;

    private LocalDateTime deletedAt;
}
