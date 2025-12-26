package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.service.ConfigManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 配置管理接口
 * 
 * 仅管理员可访问
 */
@Slf4j
@RestController
@RequestMapping("/admin/config")
@Tag(name = "配置管理", description = "动态配置管理接口（仅管理员）")
@PreAuthorize("hasRole('ADMIN', 'SUPER_ADMIN')")
public class ConfigManagementController {

    @Autowired
    private ConfigManagementService configManagementService;

    @org.springframework.beans.factory.annotation.Value("${CONFIG_ADMIN_PASSWORD:admin123}")
    private String configAdminPassword;

    /**
     * 验证配置管理密码
     */
    @PostMapping("/verify-password")
    @Operation(summary = "验证配置管理密码")
    public Result<Void> verifyPassword(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null || password.trim().isEmpty()) {
            return Result.badRequest("密码不能为空");
        }

        if (configAdminPassword.equals(password)) {
            return Result.success("密码验证成功");
        } else {
            log.warn("配置管理密码验证失败");
            return Result.error(403, "密码错误");
        }
    }

    /**
     * 获取可编辑的配置项列表（仅 Key）
     */
    @GetMapping("/keys")
    @Operation(summary = "获取可编辑配置项")
    public Result<List<String>> getEditableConfigKeys() {
        return Result.success(configManagementService.getEditableConfigKeys());
    }

    /**
     * 更新单个配置
     */
    @PutMapping("/{key}")
    @Operation(summary = "更新单个配置")
    public Result<Void> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {

        String value = body.get("value");
        if (value == null) {
            return Result.badRequest("value 不能为空");
        }

        // 先备份
        configManagementService.backupConfig();

        boolean success = configManagementService.updateConfig(key, value);
        if (success) {
            return Result.success("配置更新成功，重启应用后生效");
        } else {
            return Result.serverError("配置更新失败");
        }
    }

    /**
     * 批量更新配置
     */
    @PutMapping("/batch")
    @Operation(summary = "批量更新配置")
    public Result<Void> updateConfigs(@RequestBody Map<String, String> configs) {
        // 先备份
        configManagementService.backupConfig();

        boolean success = configManagementService.updateConfigs(configs);
        if (success) {
            return Result.success("配置批量更新成功，重启应用后生效");
        } else {
            return Result.serverError("部分配置更新失败");
        }
    }

    /**
     * 备份当前配置
     */
    @PostMapping("/backup")
    @Operation(summary = "备份当前配置")
    public Result<Void> backupConfig() {
        boolean success = configManagementService.backupConfig();
        if (success) {
            return Result.success("配置备份成功");
        } else {
            return Result.serverError("配置备份失败");
        }
    }

    /**
     * 快速更新邮箱授权码
     */
    @PutMapping("/mail-password")
    @Operation(summary = "快速更新邮箱授权码")
    public Result<Void> updateMailPassword(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null || password.trim().isEmpty()) {
            return Result.badRequest("授权码不能为空");
        }

        // 备份
        configManagementService.backupConfig();

        // 更新
        boolean success = configManagementService.updateConfig("MAIL_PASSWORD", password);
        if (success) {
            return Result.success("邮箱授权码更新成功，重启应用后生效");
        } else {
            return Result.serverError("邮箱授权码更新失败");
        }
    }

    /**
     * 重启应用
     * 
     * 注意：此接口会在3秒后重启应用，请确保已保存所有配置
     */
    @PostMapping("/restart")
    @Operation(summary = "重启应用")
    public Result<Void> restartApplication() {
        log.warn("管理员请求重启应用");

        // 异步执行重启，避免请求超时
        new Thread(() -> {
            try {
                // 等待3秒，让响应返回给前端
                Thread.sleep(3000);

                log.info("开始重启应用...");

                // 尝试多种重启方式
                boolean restarted = false;

                // 方式1: Docker Compose
                if (executeCommand("docker-compose restart viewx-backend")) {
                    log.info("通过 Docker Compose 重启成功");
                    restarted = true;
                }

                // 方式2: Systemd
                if (!restarted && executeCommand("systemctl restart viewx")) {
                    log.info("通过 Systemd 重启成功");
                    restarted = true;
                }

                // 方式3: 直接退出，让外部进程管理器重启
                if (!restarted) {
                    log.warn("未找到自动重启方式，应用将退出（需要外部进程管理器）");
                    System.exit(0);
                }

            } catch (Exception e) {
                log.error("重启应用失败", e);
            }
        }).start();

        return Result.success("应用将在3秒后重启");
    }

    /**
     * 执行系统命令
     */
    private boolean executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.debug("执行命令失败: {}", command, e);
            return false;
        }
    }
}
