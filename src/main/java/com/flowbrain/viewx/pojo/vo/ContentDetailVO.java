package com.flowbrain.viewx.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容详情 VO - 统一的内容展示对象
 */
@Data
public class ContentDetailVO {
    private Long id;
    private String contentType; // VIDEO, IMAGE, IMAGE_SET
    
    private String title;
    private String description;
    
    // 媒体信息
    private String primaryUrl;      // 主要媒体URL
    private String coverUrl;        // 封面图
    private String thumbnailUrl;    // 缩略图
    private List<String> mediaUrls; // 多媒体URL列表(图片集使用)
    
    // 视频专用
    private Integer duration;
    
    // 分类和标签
    private String category;
    private String subcategory;
    private List<String> tags;
    
    // 权限
    private String visibility;
    private String status;
    
    // 上传者信息
    private Long uploaderId;
    private String uploaderNickname;
    private String uploaderAvatar;
    private Boolean isUploaderVerified;
    
    // 统计信息
    private Long viewCount;
    private Long likeCount;
    private Long dislikeCount;
    private Long shareCount;
    private Long commentCount;
    
    // 交互状态
    private Boolean isLiked;
    private Boolean isFavorited;
    private Boolean isFollowingUploader;
    
    // 时间
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
