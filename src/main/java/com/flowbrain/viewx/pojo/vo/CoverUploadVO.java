package com.flowbrain.viewx.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 封面上传响应 VO
 * 包含封面图和缩略图的URL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoverUploadVO {
    /**
     * 封面图片URL (原始尺寸)
     */
    private String coverUrl;
    
    /**
     * 缩略图URL (320x180)
     */
    private String thumbnailUrl;
}
