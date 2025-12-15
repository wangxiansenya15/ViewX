package com.flowbrain.viewx.common.enums;

/**
 * 通知类型枚举
 */
public enum NotificationType {
    // 社交互动通知
    FOLLOW("关注了你"),
    LIKE_VIDEO("点赞了你的视频"),
    FAVORITE_VIDEO("收藏了你的视频"),
    COMMENT_VIDEO("评论了你的视频"),
    REPLY_COMMENT("回复了你的评论"),
    LIKE_COMMENT("点赞了你的评论"),

    // 系统通知
    VIDEO_APPROVED("你的视频已通过审核"),
    VIDEO_REJECTED("你的视频审核未通过"),
    SYSTEM_ANNOUNCEMENT("系统公告");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
