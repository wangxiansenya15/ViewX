package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户收藏实体类
 * 对应数据库表：user_favorites
 */
@Data
@TableName("user_favorites")
public class Favorite implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收藏记录唯一ID
     */
    @TableId(value = "favorite_id", type = IdType.AUTO)
    private Integer favoriteId;

    /**
     * 用户ID，关联user表的id字段
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 商品ID，关联product表的product_id字段
     */
    @TableField(value = "product_id")
    private Integer productId;

    /**
     * 收藏时间
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    /**
     * 商品信息（非数据库字段，用于返回给前端）
     * 通过联表查询获取
     */
    @TableField(exist = false)
    private Product product;
}