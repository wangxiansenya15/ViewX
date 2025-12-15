package com.flowbrain.viewx.pojo.dto;

import lombok.Data;

/**
 * 系统版本信息DTO
 */
@Data
public class SystemVersionDTO {
    /**
     * 当前版本号
     */
    private String version;

    /**
     * 构建时间
     */
    private String buildTime;

    /**
     * 更新日志
     */
    private String updateLog;

    /**
     * 是否强制更新
     */
    private Boolean forceUpdate;

    /**
     * 下载地址（可选）
     */
    private String downloadUrl;

    /**
     * 最低兼容版本
     */
    private String minCompatibleVersion;

    /**
     * 更新类型：MAJOR(主版本), MINOR(次版本), PATCH(补丁)
     */
    private String updateType;
}
