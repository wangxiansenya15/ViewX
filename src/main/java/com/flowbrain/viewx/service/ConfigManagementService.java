package com.flowbrain.viewx.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.*;

/**
 * 配置文件管理服务
 * 
 * 功能：
 * 1. 动态修改 .env 文件中的指定配置项
 * 2. 自动备份
 * 3. 安全控制：仅允许修改白名单内的配置
 */
@Slf4j
@Service
public class ConfigManagementService {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // .env 文件路径（项目根目录）
    private static final String ENV_FILE_PATH = ".env";

    // 允许修改的配置白名单
    private static final Set<String> SAFE_UPDATE_KEYS = new HashSet<>(Arrays.asList(
            "MAIL_USERNAME",
            "MAIL_PASSWORD",
            "GITHUB_CLIENT_ID",
            "GITHUB_CLIENT_SECRET",
            "AI_API_KEY"));

    /**
     * 获取支持修改的配置项列表（仅返回 Key，不返回 Value）
     */
    public List<String> getEditableConfigKeys() {
        return new ArrayList<>(SAFE_UPDATE_KEYS);
    }

    /**
     * 更新单个配置项
     * 
     * @param key   配置键
     * @param value 配置值
     * @return 是否成功
     */
    public boolean updateConfig(String key, String value) {
        // 安全校验：只允许修改白名单内的配置
        if (!SAFE_UPDATE_KEYS.contains(key)) {
            log.warn("尝试修改未授权的配置项: {}", key);
            return false;
        }

        try {
            Path envPath = getEnvFilePath();
            if (!Files.exists(envPath)) {
                log.error(".env 文件不存在");
                return false;
            }

            // 读取所有行
            List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
            List<String> newLines = new ArrayList<>();
            boolean found = false;

            for (String line : lines) {
                String trimmedLine = line.trim();

                // 如果是目标配置行
                if (trimmedLine.startsWith(key + "=")) {
                    // 保留原有的注释
                    String comment = "";
                    int commentIndex = line.indexOf('#');
                    if (commentIndex > 0 && commentIndex > line.indexOf('=')) {
                        comment = "  " + line.substring(commentIndex);
                    }

                    newLines.add(key + "=" + value + comment);
                    found = true;
                } else {
                    newLines.add(line);
                }
            }

            // 如果没找到，追加到文件末尾
            if (!found) {
                newLines.add(key + "=" + value);
            }

            // 写回文件
            Files.write(envPath, newLines, StandardCharsets.UTF_8);

            log.info("成功更新配置: {} = {}", key, maskSensitiveValue(key, value));
            return true;

        } catch (IOException e) {
            log.error("更新配置失败: {}", key, e);
            return false;
        }
    }

    /**
     * 批量更新配置
     */
    public boolean updateConfigs(Map<String, String> configs) {
        boolean allSuccess = true;

        for (Map.Entry<String, String> entry : configs.entrySet()) {
            if (!updateConfig(entry.getKey(), entry.getValue())) {
                allSuccess = false;
            }
        }

        return allSuccess;
    }

    /**
     * 备份当前配置
     */
    public boolean backupConfig() {
        try {
            Path envPath = getEnvFilePath();
            if (!Files.exists(envPath)) {
                return false;
            }

            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path backupPath = Paths.get(envPath.getParent().toString(), ".env.backup." + timestamp);

            Files.copy(envPath, backupPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("配置备份成功: {}", backupPath);
            return true;

        } catch (IOException e) {
            log.error("配置备份失败", e);
            return false;
        }
    }

    /**
     * 获取 .env 文件路径
     */
    private Path getEnvFilePath() {
        // 尝试多个可能的路径
        String[] possiblePaths = {
                ENV_FILE_PATH, // 当前目录
                "../" + ENV_FILE_PATH, // 父目录
                System.getProperty("user.dir") + "/" + ENV_FILE_PATH // 工作目录
        };

        for (String pathStr : possiblePaths) {
            Path path = Paths.get(pathStr);
            if (Files.exists(path)) {
                return path;
            }
        }

        // 默认返回当前目录
        return Paths.get(ENV_FILE_PATH);
    }

    /**
     * 脱敏敏感值（用于日志）
     */
    private String maskSensitiveValue(String key, String value) {
        if (key.toUpperCase().contains("PASSWORD") ||
                key.toUpperCase().contains("SECRET") ||
                key.toUpperCase().contains("KEY")) {

            if (value.length() <= 4) {
                return "****";
            }
            return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
        }
        return value;
    }
}
