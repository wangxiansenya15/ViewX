package com.flowbrain.viewx.pojo.dto;

import com.flowbrain.viewx.common.UserStatus;
import lombok.Data;
import java.util.List;

@Data
public class UserDTO {
    // 用户基础信息
    private Long id;
    private String username;

    // 安全相关字段
    private String password;
    private String token;
    
    // 权限控制字段
    private List<String> roles;
    private List<String> permissions;
    
    // 业务扩展字段
    private String avatar;
    private Integer memberLevel;
    
    // 用户状态相关字段
    private UserStatus userStatus;

    public UserDTO() {
    }

    public UserDTO(String token) {
        this.token = token;
    }

    public UserDTO(String token, String username, List<String> roles) {
        this.token = token;
        this.username = username;
        this.roles = roles;
    }
    
    // 包含用户状态信息的构造函数
    public UserDTO(Long id,String token, String username, List<String> roles, UserStatus userStatus) {
        this.id = id;
        this.token = token;
        this.username = username;
        this.roles = roles;
        this.userStatus = userStatus;
    }
}
