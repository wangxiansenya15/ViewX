package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("vx_topics")
public class Topic {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name; // 话题名称（不含#）
    private String description;

    @TableField(value = "video_count")
    private Long videoCount = 0L;

    @TableField(value = "view_count")
    private Long viewCount = 0L;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted = false;

    private LocalDateTime deletedAt;
}
