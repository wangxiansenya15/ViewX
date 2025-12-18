package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("vx_video_comments")
public class VideoComment {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long videoId;
    private Long userId;
    private Long parentId;
    private String content;

    @TableField(value = "like_count")
    private Integer likeCount = 0;

    private Boolean isPinned = false;

    @TableField(fill = FieldFill.INSERT)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    // 软删除
    @TableLogic
    private Boolean isDeleted = false;

    private LocalDateTime deletedAt;

    // For display (not in DB)
    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String nickname;

    @TableField(exist = false)
    private String avatar;
}
