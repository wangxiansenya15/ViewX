package com.flowbrain.viewx.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActionLog {
    private Long id;
    private Long userId;
    private String actionType;
    private Long videoId;
    private String ipAddress;
    private String deviceInfo;
    private LocalDateTime createdAt;
}
