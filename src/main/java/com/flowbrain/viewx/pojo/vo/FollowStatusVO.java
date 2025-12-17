package com.flowbrain.viewx.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注状态 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatusVO {
    /**
     * 当前用户是否关注了目标用户
     */
    private Boolean isFollowing;

    /**
     * 目标用户是否关注了当前用户
     */
    private Boolean isFollower;

    /**
     * 是否相互关注
     */
    private Boolean isMutual;

    /**
     * 关注状态文本（用于前端显示）
     * "关注" / "已关注" / "相互关注"
     */
    private String statusText;
}
