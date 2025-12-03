package com.flowbrain.viewx.pojo.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 评论创建 DTO
 */
@Data
public class CommentCreateDTO {
    @NotNull(message = "视频ID不能为空")
    private Long videoId;
    
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论长度不能超过1000字符")
    private String content;
    
    private Long parentId; // 回复评论时使用
}
