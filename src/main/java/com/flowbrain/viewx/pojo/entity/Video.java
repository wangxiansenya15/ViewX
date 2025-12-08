package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("vx_videos")
public class Video {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;
    private String description;
    private Integer duration;
    private String videoUrl;
    private String coverUrl;
    private String thumbnailUrl;
    private String previewUrl;
    private Long fileSize;
    private String format;
    private String resolution;
    private String category;
    private String subcategory;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> tags;

    private String visibility;
    private String status;
    private Long uploaderId;

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

    private Integer averageWatchTime;

    // AI fields
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String aiTags;

    private String contentSummary;
    private Double sentimentScore;

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
