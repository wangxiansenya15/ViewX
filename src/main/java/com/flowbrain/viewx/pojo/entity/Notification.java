package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("vx_notifications")
public class Notification {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long recipientId;
    private Long senderId;
    private String notificationType;
    private Long relatedVideoId;
    private Long relatedCommentId;
    private String content;
    
    @TableField(value = "is_read")
    private Boolean isRead = false;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 软删除
    @TableLogic
    private Boolean isDeleted = false;
    
    private LocalDateTime deletedAt;
    
    // For display (not in DB)
    @TableField(exist = false)
    private String senderNickname;
    
    @TableField(exist = false)
    private String senderAvatar;
    
    @TableField(exist = false)
    private String videoTitle;
}
