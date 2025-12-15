package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.SystemVersionDTO;
import com.flowbrain.viewx.service.SystemVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 系统版本管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
@Tag(name = "系统管理", description = "系统版本和更新相关接口")
public class SystemVersionController {

    private final SystemVersionService systemVersionService;

    @GetMapping("/version")
    @Operation(summary = "获取当前系统版本", description = "返回当前系统的版本信息")
    public Result<SystemVersionDTO> getCurrentVersion() {
        SystemVersionDTO version = systemVersionService.getCurrentVersion();
        return Result.success(version);
    }

    @GetMapping("/check-update")
    @Operation(summary = "检查更新", description = "检查是否有新版本可用")
    public Result<SystemVersionDTO> checkUpdate(
            @Parameter(description = "客户端当前版本号") @RequestParam(required = false) String clientVersion) {

        log.info("客户端检查更新，当前版本: {}", clientVersion);

        SystemVersionDTO updateInfo = systemVersionService.checkUpdate(clientVersion);

        if (updateInfo != null) {
            return Result.success("发现新版本", updateInfo);
        } else {
            return Result.success("当前已是最新版本", null);
        }
    }

    @PostMapping("/upgrade")
    @Operation(summary = "执行版本升级", description = "执行从旧版本到新版本的升级逻辑")
    public Result<Void> performUpgrade(
            @Parameter(description = "源版本号") @RequestParam String fromVersion,
            @Parameter(description = "目标版本号") @RequestParam String toVersion) {

        log.info("执行版本升级请求: {} -> {}", fromVersion, toVersion);

        boolean success = systemVersionService.performUpgrade(fromVersion, toVersion);

        if (success) {
            return Result.success("升级成功", null);
        } else {
            return Result.error(Result.SERVER_ERROR, "升级失败");
        }
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查系统运行状态")
    public Result<String> healthCheck() {
        return Result.success("OK", "系统运行正常");
    }
}
