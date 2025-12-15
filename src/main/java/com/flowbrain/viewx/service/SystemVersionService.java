package com.flowbrain.viewx.service;

import com.flowbrain.viewx.pojo.dto.SystemVersionDTO;

/**
 * 系统版本服务接口
 */
public interface SystemVersionService {

    /**
     * 获取当前系统版本信息
     * 
     * @return 版本信息
     */
    SystemVersionDTO getCurrentVersion();

    /**
     * 检查是否需要更新
     * 
     * @param clientVersion 客户端版本号
     * @return 版本信息（如果需要更新）
     */
    SystemVersionDTO checkUpdate(String clientVersion);

    /**
     * 执行版本升级逻辑
     * 
     * @param fromVersion 源版本
     * @param toVersion   目标版本
     * @return 是否升级成功
     */
    boolean performUpgrade(String fromVersion, String toVersion);
}
