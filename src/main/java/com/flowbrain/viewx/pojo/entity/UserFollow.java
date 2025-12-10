package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 用户关注关系实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("vx_user_follows")
public class UserFollow {
    private Long followerId;   // 关注者ID
    private Long followedId;   // 被关注者ID
    private LocalDateTime createdAt;
}
