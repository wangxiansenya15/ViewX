package com.flowbrain.viewx.pojo.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 视频上传/创建 DTO
 */
@Data
public class VideoUploadDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;

    @Size(max = 2000, message = "描述长度不能超过2000")
    private String description;

    @NotNull(message = "视频时长不能为空")
    @Min(value = 1, message = "视频时长必须大于0")
    private Integer duration; // 视频时长（秒）

    private String coverUrl; // 封面图片URL（前端上传封面后传入）

    private String thumbnailUrl;

    private String category;

    private String subcategory;

    private String[] tags;

    @Pattern(regexp = "PUBLIC|PRIVATE|UNLISTED", message = "可见性必须是 PUBLIC, PRIVATE 或 UNLISTED")
    private String visibility = "PUBLIC";
}
