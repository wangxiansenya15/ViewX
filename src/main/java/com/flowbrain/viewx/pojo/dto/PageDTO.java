package com.flowbrain.viewx.pojo.dto;

import lombok.Data;

// 规范分页查询参数
@Data
public class PageDTO<T> {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String orderBy;
    private T queryParams; // 泛型查询条件
}