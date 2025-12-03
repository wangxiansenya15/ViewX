package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product")
public class Product {

    @TableId(value = "product_id")
    private Integer id;

    @TableField(value = "name")
    private String name;

    @TableField(value = "price")
    private double price;

    @TableField(value = "category")
    private String category;

    @TableField(value = "count")
    private Integer stock;

    @TableField(value = "remark")
    private String description;

    @TableField(value = "image_url")
    private String imageUrl;

    @TableField(value = "rating")
    private double rating;
}
