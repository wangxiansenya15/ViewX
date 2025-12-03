package com.flowbrain.viewx.pojo.vo;

import lombok.Data;

/**
 * 用户简要信息 VO (用于评论、点赞等场景)
 */
@Data
public class UserBriefVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Boolean isVerified;
}
