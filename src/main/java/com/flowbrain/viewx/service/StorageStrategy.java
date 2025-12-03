package com.flowbrain.viewx.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageStrategy {
    /**
     * 存储文件
     */
    String storeFile(MultipartFile file, String filename) throws IOException;

    /**
     * 删除文件
     */
    boolean deleteFile(String fileUrl) throws IOException;

    /**
     * 获取文件访问URL
     */
    String getFileUrl(String filename);

    /**
     * 获取存储类型
     */
    StorageType getStorageType();
}

