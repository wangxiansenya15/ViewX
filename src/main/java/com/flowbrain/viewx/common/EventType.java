package com.flowbrain.viewx.common;

/**
 * MQ 事件类型常量
 */
public class EventType {

    // ==================== 用户行为事件 ====================
    /**
     * 视频播放事件
     */
    public static final String VIDEO_PLAY = "video.play";

    /**
     * 视频点赞事件
     */
    public static final String VIDEO_LIKE = "video.like";

    /**
     * 视频取消点赞事件
     */
    public static final String VIDEO_UNLIKE = "video.unlike";

    /**
     * 视频收藏事件
     */
    public static final String VIDEO_FAVORITE = "video.favorite";

    /**
     * 视频分享事件
     */
    public static final String VIDEO_SHARE = "video.share";

    /**
     * 评论发表事件
     */
    public static final String COMMENT_CREATE = "comment.create";

    /**
     * 评论删除事件
     */
    public static final String COMMENT_DELETE = "comment.delete";

    /**
     * 用户关注事件
     */
    public static final String USER_FOLLOW = "user.follow";

    /**
     * 用户取消关注事件
     */
    public static final String USER_UNFOLLOW = "user.unfollow";

    // ==================== 内容管理事件 ====================
    /**
     * 视频上传成功事件
     */
    public static final String VIDEO_UPLOAD = "video.upload";

    /**
     * 视频审核通过事件
     */
    public static final String VIDEO_APPROVED = "video.approved";

    /**
     * 视频审核拒绝事件
     */
    public static final String VIDEO_REJECTED = "video.rejected";

    /**
     * 视频删除事件
     */
    public static final String VIDEO_DELETE = "video.delete";

    // ==================== 系统事件 ====================
    /**
     * 用户注册事件
     */
    public static final String USER_REGISTER = "user.register";

    /**
     * 用户登录事件
     */
    public static final String USER_LOGIN = "user.login";

    /**
     * 用户信息更新事件
     */
    public static final String USER_UPDATE = "user.update";
}
