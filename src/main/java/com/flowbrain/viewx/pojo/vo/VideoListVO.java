package com.flowbrain.viewx.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 视频列表 VO (精简版，用于列表展示)
 */
@Data
public class VideoListVO {
    private Long id;
    private String title;
    private String description;
    private String coverUrl;
    private String thumbnailUrl;
    private Integer duration;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private LocalDateTime publishedAt;

    // 上传者信息
    private Long uploaderId;
    private String uploaderNickname;
    private String uploaderAvatar;
}
