package com.flowbrain.viewx.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 用户资料展示 VO
 * 用途：返回给前端展示用户个人信息
 * 特点：
 * 1. 只包含前端需要展示的字段
 * 2. 隐藏敏感信息（密码、内部ID等）
 * 3. 可以包含计算字段（如粉丝数、关注数）
 * 4. 字段命名对前端友好
 */
@Data
public class UserProfileVO {

    private Long userId;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private String avatarUrl;

    private String description;

    private Integer age;

    private String gender;

    private String address;

    private String role; // 角色名称（如 "普通用户"、"管理员"）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;

    // 扩展字段：统计信息（可选）
    private Integer followersCount; // 粉丝数

    private Integer followingCount; // 关注数

    private Integer videoCount; // 视频数

    private Integer likeCount; // 获赞数
}
