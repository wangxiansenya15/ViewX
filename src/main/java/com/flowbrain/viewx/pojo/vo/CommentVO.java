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
    private String userNickname;
    private String userAvatar;
    private String content;
    private Integer likeCount;
    private Boolean isPinned;
    private LocalDateTime createdAt;
    
    // 当前用户是否点赞了该评论
    private Boolean isLiked;
    
    // 回复列表 (嵌套评论)
    private List<CommentVO> replies;
    private Integer replyCount;
}
