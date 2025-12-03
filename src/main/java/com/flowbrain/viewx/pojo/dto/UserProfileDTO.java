package com.flowbrain.viewx.pojo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户资料更新 DTO
 * 用途：接收前端传来的用户资料修改请求
 * 特点：
 * 1. 只包含允许用户修改的字段
 * 2. 包含验证注解
 * 3. 不包含敏感字段（如密码、角色等）
 */
@Data
public class UserProfileDTO {
    
    @Size(min = 2, max = 20, message = "昵称长度必须在2-20个字符之间")
    private String nickname;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Size(max = 11, message = "手机号长度不能超过11位")
    private String phone;
    
    @Size(max = 200, message = "个人简介不能超过200字")
    private String description;
    
    private Integer age;
    
    private String gender; // "MALE", "FEMALE", "OTHER"
    
    @Size(max = 100, message = "地址长度不能超过100字")
    private String address;
    
    // 头像URL（通常由单独的上传接口处理，这里只是更新URL）
    private String avatarUrl;
}
