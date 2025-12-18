package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("vx_user_details")
public class UserDetail {
    @TableField(value = "id")
    private Long id;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "description")
    private String description;

    @TableField(value = "avatar_url")
    private String avatarUrl;

    @TableField(value = "age")
    private Integer age;

    @TableField(value = "gender")
    private String gender;

    @TableField(value = "address")
    private String address;
}
