package com.flowbrain.viewx.pojo.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 视频上传/创建 DTO
 */
@Data
public class VideoCreateDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;
    
    @Size(max = 2000, message = "描述长度不能超过2000")
    private String description;
    
    @NotBlank(message = "视频URL不能为空")
    private String videoUrl;
    
    private String thumbnailUrl;
    
    private String category;
    
    private String subcategory;
    
    private String[] tags;
    
    @Pattern(regexp = "PUBLIC|PRIVATE|UNLISTED", message = "可见性必须是 PUBLIC, PRIVATE 或 UNLISTED")
    private String visibility = "PUBLIC";
}
