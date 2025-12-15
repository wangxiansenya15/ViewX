package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.pojo.dto.SystemVersionDTO;
import com.flowbrain.viewx.service.SystemVersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 系统版本服务实现
 */
@Slf4j
@Service
public class SystemVersionServiceImpl implements SystemVersionService {

    @Value("${app.version:1.0.0}")
    private String currentVersion;

    @Value("${app.build-time:}")
    private String buildTime;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public SystemVersionDTO getCurrentVersion() {
        SystemVersionDTO versionDTO = new SystemVersionDTO();
        versionDTO.setVersion(currentVersion);
        versionDTO.setBuildTime(buildTime.isEmpty() ? LocalDateTime.now().format(FORMATTER) : buildTime);
        versionDTO.setForceUpdate(false);
        versionDTO.setMinCompatibleVersion("1.0.0");
        versionDTO.setUpdateType(determineUpdateType(currentVersion));

        // 可以从配置文件或数据库读取更新日志
        versionDTO.setUpdateLog(getUpdateLog(currentVersion));

        return versionDTO;
    }

    @Override
    public SystemVersionDTO checkUpdate(String clientVersion) {
        if (clientVersion == null || clientVersion.isEmpty()) {
            return getCurrentVersion();
        }

        // 比较版本号
        if (isNewerVersion(currentVersion, clientVersion)) {
            SystemVersionDTO updateInfo = getCurrentVersion();

            // 判断是否强制更新（例如：跨主版本必须强制更新）
            updateInfo.setForceUpdate(isMajorVersionChange(clientVersion, currentVersion));

            log.info("客户端版本 {} 需要更新到 {}", clientVersion, currentVersion);
            return updateInfo;
        }

        return null; // 无需更新
    }

    @Override
    public boolean performUpgrade(String fromVersion, String toVersion) {
        log.info("开始执行版本升级: {} -> {}", fromVersion, toVersion);

        try {
            // 根据版本号执行不同的升级逻辑
            if (needsUpgrade(fromVersion, "1.0.0", toVersion)) {
                upgradeFrom1_0_0();
            }

            if (needsUpgrade(fromVersion, "1.1.0", toVersion)) {
                upgradeFrom1_1_0();
            }

            if (needsUpgrade(fromVersion, "2.0.0", toVersion)) {
                upgradeFrom2_0_0();
            }

            log.info("版本升级完成: {} -> {}", fromVersion, toVersion);
            return true;
        } catch (Exception e) {
            log.error("版本升级失败: {} -> {}", fromVersion, toVersion, e);
            return false;
        }
    }

    /**
     * 判断是否需要执行某个版本的升级逻辑
     */
    private boolean needsUpgrade(String fromVersion, String upgradeVersion, String toVersion) {
        return compareVersion(fromVersion, upgradeVersion) < 0
                && compareVersion(toVersion, upgradeVersion) >= 0;
    }

    /**
     * 从 1.0.0 升级的逻辑
     */
    private void upgradeFrom1_0_0() {
        log.info("执行 1.0.0 版本升级逻辑");
        // 示例：数据库表结构变更、数据迁移等
        // 例如：添加新字段、修改索引等
    }

    /**
     * 从 1.1.0 升级的逻辑
     */
    private void upgradeFrom1_1_0() {
        log.info("执行 1.1.0 版本升级逻辑");
        // 示例：新功能相关的数据初始化
    }

    /**
     * 从 2.0.0 升级的逻辑
     */
    private void upgradeFrom2_0_0() {
        log.info("执行 2.0.0 版本升级逻辑");
        // 示例：重大架构调整相关的迁移
    }

    /**
     * 比较两个版本号
     * 
     * @return 负数表示 v1 < v2，0 表示相等，正数表示 v1 > v2
     */
    private int compareVersion(String v1, String v2) {
        if (v1 == null || v2 == null) {
            return 0;
        }

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (num1 != num2) {
                return num1 - num2;
            }
        }

        return 0;
    }

    /**
     * 解析版本号部分（处理可能的非数字字符）
     */
    private int parseVersionPart(String part) {
        try {
            // 移除非数字字符（如 "1.0.0-beta" 中的 "-beta"）
            String numericPart = part.replaceAll("[^0-9].*", "");
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 判断是否为更新的版本
     */
    private boolean isNewerVersion(String newVersion, String oldVersion) {
        return compareVersion(newVersion, oldVersion) > 0;
    }

    /**
     * 判断是否为主版本变更
     */
    private boolean isMajorVersionChange(String oldVersion, String newVersion) {
        String[] oldParts = oldVersion.split("\\.");
        String[] newParts = newVersion.split("\\.");

        if (oldParts.length > 0 && newParts.length > 0) {
            int oldMajor = parseVersionPart(oldParts[0]);
            int newMajor = parseVersionPart(newParts[0]);
            return newMajor > oldMajor;
        }

        return false;
    }

    /**
     * 确定更新类型
     */
    private String determineUpdateType(String version) {
        String[] parts = version.split("\\.");
        if (parts.length < 3) {
            return "UNKNOWN";
        }

        int patch = parseVersionPart(parts[2]);
        int minor = parseVersionPart(parts[1]);

        if (patch > 0) {
            return "PATCH";
        } else if (minor > 0) {
            return "MINOR";
        } else {
            return "MAJOR";
        }
    }

    /**
     * 获取更新日志（可以从配置文件或数据库读取）
     */
    private String getUpdateLog(String version) {
        // 这里可以从配置文件、数据库或外部文件读取
        // 示例：硬编码的更新日志
        switch (version) {
            case "1.0.0":
                return "- 初始版本发布\n- 基础功能实现";
            case "1.0.1":
                return "- 修复若干已知问题\n- 性能优化";
            case "1.1.0":
                return "- 新增暗色模式\n- 优化用户界面\n- 修复已知bug";
            case "2.0.0":
                return "- 全新架构升级\n- 性能大幅提升\n- 新增多项功能";
            default:
                return "- 系统优化和bug修复";
        }
    }
}
