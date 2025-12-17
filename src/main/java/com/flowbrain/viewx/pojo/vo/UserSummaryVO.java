package com.flowbrain.viewx.pojo.vo;

import lombok.Data;

@Data
public class UserSummaryVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Boolean isFollowing; // whether the current user follows this user
}
