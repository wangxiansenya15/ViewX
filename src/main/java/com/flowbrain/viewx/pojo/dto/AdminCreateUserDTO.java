package com.flowbrain.viewx.pojo.dto;

import lombok.Data;

/**
 * 管理员创建用户 DTO
 */
@Data
public class AdminCreateUserDTO {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String nickname;
    private String role; // USER, ADMIN, SUPER_ADMIN, REVIEWER
}
