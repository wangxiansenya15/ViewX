package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.config.ResourceStorageConfig;
import com.flowbrain.viewx.service.StorageStrategy;
import com.flowbrain.viewx.common.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class LocalStorageStrategy implements StorageStrategy {

    private final Path storageLocation;
    private final String baseUrl;

    public LocalStorageStrategy(ResourceStorageConfig config) {
        this.storageLocation = Paths.get(config.getUploadDir()).toAbsolutePath().normalize();
        this.baseUrl = config.getBaseUrl();

        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("无法创建文件存储目录", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String filename) throws IOException {
        Path targetLocation = storageLocation.resolve(filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        log.info("文件存储到本地: {}", filename);
        return filename;
    }

    @Override
    public boolean deleteFile(String fileUrl) throws IOException {
        String filename = extractFilenameFromUrl(fileUrl);
        Path filePath = storageLocation.resolve(filename);
        return Files.deleteIfExists(filePath);
    }

    @Override
    public String getFileUrl(String filename) {
        return baseUrl + "/" + filename;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    private String extractFilenameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new IllegalArgumentException("文件URL不能为空");
        }

        int lastSlashIndex = fileUrl.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            // 如果没有斜杠，说明可能已经是文件名
            return fileUrl;
        }

        String filename = fileUrl.substring(lastSlashIndex + 1);
        if (filename.isEmpty()) {
            throw new IllegalArgumentException("无法从URL中提取文件名: " + fileUrl);
        }

        return filename;
    }
}
