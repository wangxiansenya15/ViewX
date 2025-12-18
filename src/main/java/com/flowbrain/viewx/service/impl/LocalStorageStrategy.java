package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.config.ResourceStorageConfig;
import com.flowbrain.viewx.service.StorageStrategy;
import com.flowbrain.viewx.common.enums.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件存储策略实现类
 * 
 * 该类实现了 StorageStrategy 接口，提供本地文件系统的文件存储功能。
 * 文件将被存储在服务器的本地磁盘上，适用于单机部署或开发环境。
 * 
 * 主要功能：
 * - 上传文件到本地文件系统
 * - 删除本地文件
 * - 生成文件访问URL
 * 
 * @author ViewX Team
 * @see StorageStrategy
 * @see com.flowbrain.viewx.service.impl.LocalStorageStrategy
 */
@Service
@Slf4j
public class LocalStorageStrategy implements StorageStrategy {

    /**
     * 文件存储的根目录路径
     * 所有上传的文件都会保存在这个目录下
     */
    private final Path storageLocation;

    /**
     * 文件访问的基础URL
     * 用于生成文件的完整访问地址，例如：http://localhost:8080/uploads
     */
    private final String baseUrl;

    /**
     * 构造函数：初始化本地存储策略
     * 
     * @param config 资源存储配置对象，包含上传目录和基础URL等配置信息
     * @throws RuntimeException 如果无法创建存储目录
     */
    public LocalStorageStrategy(ResourceStorageConfig config) {
        // 获取上传目录的绝对路径并规范化（移除冗余的路径分隔符和 . 或 .. 元素）
        this.storageLocation = Paths.get(config.getUploadDir()).toAbsolutePath().normalize();
        this.baseUrl = config.getBaseUrl();

        try {
            // 如果存储目录不存在，则创建它（包括所有必需的父目录）
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("无法创建文件存储目录", e);
        }
    }

    /**
     * 存储文件到本地文件系统
     * 
     * @param file     要存储的文件（MultipartFile 对象，通常来自HTTP请求）
     * @param filename 文件名（包含相对路径，例如：avatars/user123.jpg）
     * @return 存储后的文件名（与输入的 filename 相同）
     * @throws IOException 如果文件存储过程中发生IO错误
     */
    @Override
    public String storeFile(MultipartFile file, String filename) throws IOException {
        // 解析目标文件的完整路径（存储根目录 + 文件名）
        Path targetLocation = storageLocation.resolve(filename);

        // 创建父目录（如果不存在）
        // 例如：如果 filename 是 "avatars/user123.jpg"，则创建 "avatars" 目录
        Files.createDirectories(targetLocation.getParent());

        // 将上传的文件复制到目标位置
        // REPLACE_EXISTING：如果文件已存在，则覆盖它
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        log.info("文件存储到本地: {}", filename);
        return filename;
    }

    @Override
    public String storeFile(java.io.InputStream inputStream, String filename) throws IOException {
        Path targetLocation = storageLocation.resolve(filename);
        Files.createDirectories(targetLocation.getParent());
        Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        log.info("文件存储到本地(Stream): {}", filename);
        return filename;
    }

    /**
     * 删除本地文件
     * 
     * @param fileUrl 文件的URL地址（可以是完整URL或相对路径）
     * @return true 如果文件被成功删除，false 如果文件不存在
     * @throws IOException 如果删除过程中发生IO错误
     */
    @Override
    public boolean deleteFile(String fileUrl) throws IOException {
        // 从URL中提取相对路径
        String relativePath = extractRelativePathFromUrl(fileUrl);

        // 构建文件的完整路径
        Path filePath = storageLocation.resolve(relativePath);

        // 删除文件（如果存在）
        return Files.deleteIfExists(filePath);
    }

    /**
     * 生成文件的访问URL
     * 
     * @param filename 文件名（相对路径）
     * @return 文件的完整访问URL
     *         例如：输入 "avatars/user123.jpg"，返回
     *         "http://localhost:8080/uploads/avatars/user123.jpg"
     */
    @Override
    public String getFileUrl(String filename) {
        return baseUrl + "/" + filename;
    }

    /**
     * 获取存储类型
     * 
     * @return 返回 StorageType.LOCAL，表示本地存储
     */
    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    /**
     * 获取存储根目录的绝对路径
     * 
     * @return 存储根目录的绝对路径字符串
     */
    public String getStorageRoot() {
        return storageLocation.toString();
    }

    /**
     * 从文件URL中提取相对路径
     * 
     * 该方法处理多种URL格式：
     * 1. 完整URL（包含baseUrl）：http://localhost:8080/uploads/avatars/user123.jpg ->
     * avatars/user123.jpg
     * 2. 相对路径：avatars/user123.jpg -> avatars/user123.jpg
     * 3. 文件名：user123.jpg -> user123.jpg
     * 
     * @param fileUrl 文件的URL或路径
     * @return 提取出的相对路径
     * @throws IllegalArgumentException 如果URL为空或无法提取路径
     */
    private String extractRelativePathFromUrl(String fileUrl) {
        // 验证输入参数
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new IllegalArgumentException("文件URL不能为空");
        }

        // 情况1：URL包含baseUrl前缀，需要移除它
        if (fileUrl.startsWith(baseUrl)) {
            // 截取baseUrl之后的部分
            String relativePath = fileUrl.substring(baseUrl.length());

            // 移除开头的斜杠（如果有）
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            return relativePath;
        }

        // 情况2：URL不包含baseUrl，尝试从最后一个斜杠后提取文件名
        int lastSlashIndex = fileUrl.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            // 如果没有斜杠，说明可能已经是相对路径或纯文件名
            return fileUrl;
        }

        // 提取最后一个斜杠之后的部分
        String path = fileUrl.substring(lastSlashIndex + 1);
        if (path.isEmpty()) {
            throw new IllegalArgumentException("无法从URL中提取路径: " + fileUrl);
        }

        return path;
    }
}
