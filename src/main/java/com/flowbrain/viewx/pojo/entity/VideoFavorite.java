package com.flowbrain.viewx.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VideoFavorite {
    private Long userId;
    private Long videoId;
    private LocalDateTime createdAt;
}
