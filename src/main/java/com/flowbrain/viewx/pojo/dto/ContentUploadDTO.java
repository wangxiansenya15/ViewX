package com.flowbrain.viewx.pojo.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 内容上传 DTO - 支持图片、图片集等
 */
@Data
public class ContentUploadDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;

    @Size(max = 2000, message = "描述长度不能超过2000")
    private String description;

    // 内容类型: IMAGE, IMAGE_SET
    @NotBlank(message = "内容类型不能为空")
    @Pattern(regexp = "IMAGE|IMAGE_SET", message = "内容类型必须是 IMAGE 或 IMAGE_SET")
    private String contentType;

    private String category;
    private String subcategory;
    private String[] tags;

    @Pattern(regexp = "PUBLIC|PRIVATE|UNLISTED", message = "可见性必须是 PUBLIC, PRIVATE 或 UNLISTED")
    private String visibility = "PUBLIC";
}
