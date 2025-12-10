package com.flowbrain.viewx.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容列表项 VO - 用于列表展示
 */
@Data
public class ContentVO {
    private Long id;
    private String contentType; // VIDEO, IMAGE, IMAGE_SET
    
    private String title;
    private String description;
    
    private String thumbnailUrl;    // 缩略图
    private String coverUrl;        // 封面图
    private Integer imageCount;     // 图片集的图片数量
    
    // 视频专用
    private Integer duration;
    
    // 上传者信息
    private Long uploaderId;
    private String uploaderNickname;
    private String uploaderAvatar;
    
    // 统计信息
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    
    // 时间
    private LocalDateTime publishedAt;
}
