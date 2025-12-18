package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 商店信息实体类
 * 映射数据库shop表，用于管理商店基本信息和营业状态
 */
@Data
@TableName("shop")
public class StoreInfo {
    /**
     * 商店ID，主键
     */
    @TableId(value = "id")
    private Integer id;

    /**
     * 商店名称
     */
    @TableField(value = "store_name")
    private String name;

    /**
     * 商店地址
     */
    @TableField(value = "address")
    private String address;

    /**
     * 联系电话
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 营业状态：营业中、休息中、节假日休息、系统维护中
     */
    @TableField(value = "status")
    private String status;

    /**
     * 营业开始时间
     */
    @TableField(value = "open_time")
    private LocalTime openTime;

    /**
     * 营业结束时间
     */
    @TableField(value = "close_time")
    private LocalTime closeTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;
}
