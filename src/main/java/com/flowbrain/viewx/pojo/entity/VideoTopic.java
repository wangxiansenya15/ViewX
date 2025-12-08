package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("vx_video_topics")
public class VideoTopic {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long videoId;
    private Long topicId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
