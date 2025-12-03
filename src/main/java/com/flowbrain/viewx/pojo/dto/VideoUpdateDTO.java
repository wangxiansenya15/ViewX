package com.flowbrain.viewx.pojo.dto;

import lombok.Data;

/**
 * 视频更新 DTO
 */
@Data
public class VideoUpdateDTO {
    private String title;
    private String description;
    private String thumbnailUrl;
    private String category;
    private String subcategory;
    private String[] tags;
    private String visibility;
}
