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
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    // 软删除
    @TableLogic
    private Boolean isDeleted = false;
    
    private LocalDateTime deletedAt;
    
    // For display (not in DB)
    @TableField(exist = false)
    private String userNickname;
    
    @TableField(exist = false)
    private String userAvatar;
}
