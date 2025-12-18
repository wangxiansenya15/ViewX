package com.flowbrain.viewx.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 视频审核 VO
 * 用于管理员审核视频时展示的信息
 */
@Data
public class VideoReviewVO {
    private Long id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private String coverUrl;
    private Integer duration;
    private String category;
    private String subcategory;
    private String[] tags;

    // 上传者信息
    private Long uploaderId;
    private String uploaderUsername;
    private String uploaderNickname;
    private String uploaderAvatar;

    // 审核相关
    private String status; // PENDING, APPROVED, REJECTED
    private String visibility;

    // 统计信息
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;

    // 时间信息
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
