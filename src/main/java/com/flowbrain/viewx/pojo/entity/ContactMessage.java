package com.flowbrain.viewx.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName("contact_messages")
@Data
public class ContactMessage {
    @TableId(value = "msg_id")
    private Integer msg_id;

    @TableField(value = "username")
    private String username;

    @TableField(value = "email")
    private String email;

    @TableField(value = "message")
    private String message;

    @TableField(value = "problem")
    private String problem;

    @TableField(value = "status")
    private Integer status;
}
