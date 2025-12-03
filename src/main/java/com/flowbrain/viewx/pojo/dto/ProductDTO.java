package com.flowbrain.viewx.pojo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDTO {
    // 商品基础信息
    private Long id;
    private String productName;
    private String description;
    
    // 价格信息（使用BigDecimal保证精度）
    private BigDecimal price;
    private BigDecimal discountPrice;
    
    // 库存信息
    private Integer stock;
    private Integer soldCount;
    
    // 分类关联
    private Long categoryId;
    private String categoryName;
    
    // 商品状态（1-上架 0-下架）
    private Integer status;
    
    // 图片信息
    private String mainImage;
    private List<String> subImages;
}