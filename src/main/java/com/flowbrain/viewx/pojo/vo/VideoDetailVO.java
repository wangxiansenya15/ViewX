package com.flowbrain.viewx.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频详情 VO (完整版，用于视频播放页)
 */
@Data
public class VideoDetailVO {
    private Long id;
    private String title;
    private String description;
    private Integer duration;
    private String videoUrl;
    private String coverUrl;
    private String thumbnailUrl;
    private String resolution;
    private String category;
    private String subcategory;
    private List<String> tags;

    // 统计数据
    private Long viewCount;
    private Long likeCount;
    private Long dislikeCount;
    private Long shareCount;
    private Long commentCount;

    // AI 分析结果
    private String contentSummary;
    private List<String> aiTags;
    private Double sentimentScore;

    // 上传者信息
    private Long uploaderId;
    private String uploaderNickname;
    private String uploaderAvatar;
    private Boolean isUploaderVerified;

    // 用户交互状态 (当前登录用户)
    private Boolean isLiked;
    private Boolean isFavorited;
    private Boolean isFollowingUploader;

    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
