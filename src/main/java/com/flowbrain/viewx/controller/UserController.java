package com.flowbrain.viewx.controller;


import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.common.UserStatus;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/users")
public class UserController {
    @Autowired
    private UserService userService;

    // 创建用户（POST /users）
    @PostMapping
    public Result<?> insertUser(@RequestBody User user) {
        userService.insertUser(user);
        return Result.success("用户创建成功", user);
    }

    // 删除用户（DELETE /users/{id}）
    @DeleteMapping("/{id}")
    public Result<?> deleteUserById(@PathVariable Long id) {
        if (id == null) {
            return Result.badRequest("ID不能为空");
        }
        if (userService.getUserById(id) == null) {
            return Result.notFound("用户不存在");
        }
        return userService.deleteUserById(id);
    }

    // 更新用户（PUT /users/{id}）
    @PutMapping("/{id}")
    public Result<?> updateUser(@RequestBody User user, @PathVariable Integer id) {
        if (!id.equals(user.getId())) {
            return Result.badRequest("ID不匹配");
        }
        return userService.updateUser(user);
    }

    
    // 获取所有用户（GET /users）

    public Result<List<User>> getAllUsers() {
        return Result.success(userService.getAllUsers());
    }


    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size) {
        return userService.getUserList(page, size);
    }


    // 获取单个用户（GET /users/{id}）
    @GetMapping("/{id}")
    public Result<?> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // 更新用户状态（PATCH /users/{id}/status）
    @PatchMapping("/{id}/status")
    public Result<?> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Object> statusData) {
        // 从请求体中获取状态信息
        log.info("接收到用户状态更新请求，用户ID: {}, 状态数据: {}", id, statusData);
        
        // 获取状态值，如果前端直接传了status枚举字符串
        UserStatus status = null;
        if (statusData.containsKey("status") && statusData.get("status") instanceof String) {
            try {
                status = UserStatus.valueOf((String) statusData.get("status"));
            } catch (IllegalArgumentException e) {
                log.warn("无效的状态值: {}", statusData.get("status"));
                return Result.badRequest("无效的状态值");
            }
        }

        // 如果没有直接传status枚举，则从布尔值构建
        if (status == null) {
            boolean enabled = true;
            boolean accountNonExpired = true;
            boolean accountNonLocked = true;
            boolean credentialsNonExpired = true;

            if (statusData.containsKey("enabled") && statusData.get("enabled") instanceof Boolean) {
                enabled = (Boolean) statusData.get("enabled");
            }
            if (statusData.containsKey("accountNonExpired") && statusData.get("accountNonExpired") instanceof Boolean) {
                accountNonExpired = (Boolean) statusData.get("accountNonExpired");
            }
            if (statusData.containsKey("accountNonLocked") && statusData.get("accountNonLocked") instanceof Boolean) {
                accountNonLocked = (Boolean) statusData.get("accountNonLocked");
            }
            if (statusData.containsKey("credentialsNonExpired") && statusData.get("credentialsNonExpired") instanceof Boolean) {
                credentialsNonExpired = (Boolean) statusData.get("credentialsNonExpired");
            }

            status = UserStatus.fromBooleans(enabled, accountNonExpired, accountNonLocked, credentialsNonExpired);
        }

        log.info("解析后的用户状态: {}", status);
        return userService.updateStatus(id, status);
    }
}