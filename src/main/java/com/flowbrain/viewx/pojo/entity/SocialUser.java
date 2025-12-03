package com.flowbrain.viewx.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SocialUser {
    private Long id;
    private Long userId;
    private String provider;
    private String providerUserId;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
