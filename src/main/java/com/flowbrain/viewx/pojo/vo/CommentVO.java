package com.flowbrain.viewx.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论 VO (包含嵌套回复)
 */
@Data
public class CommentVO {
    private Long id;
    private Long videoId;
    private Long userId;
    private String username; // 用户名
    private String nickname; // 昵称
    private String avatar; // 头像URL
    private String content;
    private Integer likeCount;
    private Boolean isPinned;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    // 当前用户是否点赞了该评论
    private Boolean isLiked;

    // 回复列表 (嵌套评论)
    private List<CommentVO> replies;
    private Integer replyCount;
}
