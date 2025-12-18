package com.flowbrain.viewx.pojo.dto;

import lombok.Data;

import java.util.List;

// 支持无限级分类结构
@Data
public class CategoryDTO {
    private Long id;
    private String categoryName;
    private Long parentId;
    private Integer sortOrder;
    private List<CategoryDTO> children;
}