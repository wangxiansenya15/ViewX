package com.flowbrain.viewx.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件路径工具类
 * 
 * 统一管理文件存储路径结构：
 * storage/
 * ├── images/ # 图片目录
 * │ ├── avatars/ # 头像
 * │ │ └── {userId}/
 * │ │ └── main.png
 * │ ├── backgrounds/ # 背景图
 * │ │ └── {userId}/
 * │ │ └── bg_{timestamp}.jpg
 * │ └── posts/ # 图文内容
 * │ └── {userId}/
 * │ └── {postId}/
 * │ ├── image_1.jpg
 * │ ├── image_2.jpg
 * │ └── ...
 * └── videos/ # 视频目录
 * └── {userId}/
 * └── {videoId}/
 * ├── source.mp4
 * ├── cover.jpg
 * ├── thumb.jpg
 * └── segments/
 * 
 * 优点：
 * 1. 图片和视频分离，职责清晰
 * 2. 按用户ID分文件夹，防止单目录文件过多
 * 3. 支持图文内容（posts）和视频内容
 * 4. 支持后续HLS切片等扩展
 */
@Slf4j
public class FilePathUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ==================== 图片相关路径 ====================

    /**
     * 生成头像文件路径
     * 格式: images/avatars/{userId}/main.png
     * 
     * @param userId    用户ID
     * @param extension 文件扩展名（如 .png, .jpg）
     * @return 头像文件路径
     */
    public static String generateAvatarPath(Long userId, String extension) {
        return String.format("images/avatars/%d/main%s", userId, ensureExtension(extension));
    }

    /**
     * 生成背景图片文件路径
     * 格式: images/backgrounds/{userId}/bg_{timestamp}.jpg
     * 
     * @param userId    用户ID
     * @param extension 文件扩展名
     * @return 背景图片文件路径
     */
    public static String generateBackgroundPath(Long userId, String extension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return String.format("images/backgrounds/%d/bg_%s%s", userId, timestamp, ensureExtension(extension));
    }

    /**
     * 生成图文内容图片路径
     * 格式: images/posts/{userId}/{postId}/image_{index}.jpg
     * 
     * @param userId    用户ID
     * @param postId    图文内容ID
     * @param index     图片索引（从1开始）
     * @param extension 文件扩展名
     * @return 图文内容图片路径
     */
    public static String generatePostImagePath(Long userId, Long postId, int index, String extension) {
        return String.format("images/posts/%d/%d/image_%d%s", userId, postId, index, ensureExtension(extension));
    }

    /**
     * 生成图文内容目录路径
     * 格式: images/posts/{userId}/{postId}/
     * 
     * @param userId 用户ID
     * @param postId 图文内容ID
     * @return 图文内容目录路径
     */
    public static String generatePostDirectoryPath(Long userId, Long postId) {
        return String.format("images/posts/%d/%d", userId, postId);
    }

    // ==================== 视频相关路径 ====================

    /**
     * 生成视频源文件路径
     * 格式: videos/{userId}/{videoId}/source.mp4
     * 
     * @param userId    用户ID
     * @param videoId   视频ID
     * @param extension 文件扩展名
     * @return 视频源文件路径
     */
    public static String generateVideoSourcePath(Long userId, Long videoId, String extension) {
        return String.format("videos/%d/%d/source%s", userId, videoId, ensureExtension(extension));
    }

    /**
     * 生成视频封面路径
     * 格式: videos/{userId}/{videoId}/cover.jpg
     * 
     * @param userId    用户ID
     * @param videoId   视频ID
     * @param extension 文件扩展名
     * @return 视频封面路径
     */
    public static String generateVideoCoverPath(Long userId, Long videoId, String extension) {
        return String.format("videos/%d/%d/cover%s", userId, videoId, ensureExtension(extension));
    }

    /**
     * 生成视频缩略图路径
     * 格式: videos/{userId}/{videoId}/thumb.jpg
     * 
     * @param userId    用户ID
     * @param videoId   视频ID
     * @param extension 文件扩展名
     * @return 视频缩略图路径
     */
    public static String generateVideoThumbnailPath(Long userId, Long videoId, String extension) {
        return String.format("videos/%d/%d/thumb%s", userId, videoId, ensureExtension(extension));
    }

    /**
     * 生成视频HLS切片目录路径
     * 格式: videos/{userId}/{videoId}/segments/
     * 
     * @param userId  用户ID
     * @param videoId 视频ID
     * @return HLS切片目录路径
     */
    public static String generateVideoSegmentsPath(Long userId, Long videoId) {
        return String.format("videos/%d/%d/segments", userId, videoId);
    }

    /**
     * 生成视频目录路径（包含所有相关文件）
     * 格式: videos/{userId}/{videoId}/
     * 
     * @param userId  用户ID
     * @param videoId 视频ID
     * @return 视频目录路径
     */
    public static String generateVideoDirectoryPath(Long userId, Long videoId) {
        return String.format("videos/%d/%d", userId, videoId);
    }

    // ==================== 临时文件路径 ====================

    /**
     * 生成临时文件路径（用于上传过程中的临时存储）
     * 格式: temp/{userId}/{date}/{uuid}.tmp
     * 
     * @param userId 用户ID
     * @return 临时文件路径
     */
    public static String generateTempFilePath(Long userId) {
        String date = LocalDateTime.now().format(DATE_FORMATTER);
        String uuid = UUID.randomUUID().toString();
        return String.format("temp/%d/%s/%s.tmp", userId, date, uuid);
    }

    // ==================== 工具方法 ====================

    /**
     * 从原始文件名提取扩展名
     * 
     * @param originalFilename 原始文件名
     * @return 扩展名（包含点，如 .jpg）
     */
    public static String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "";
        }

        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == originalFilename.length() - 1) {
            return "";
        }

        return originalFilename.substring(lastDotIndex);
    }

    /**
     * 确保扩展名以点开头
     * 
     * @param extension 扩展名
     * @return 规范化的扩展名
     */
    private static String ensureExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "";
        }
        return extension.startsWith(".") ? extension : "." + extension;
    }

    /**
     * 获取文件所属的用户ID（从路径中解析）
     * 
     * @param filePath 文件路径
     * @return 用户ID，如果无法解析则返回null
     */
    public static Long extractUserIdFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        try {
            String[] parts = filePath.split("/");

            // images/avatars/{userId}/... 或 images/backgrounds/{userId}/...
            if (parts.length >= 3 && "images".equals(parts[0])) {
                return Long.parseLong(parts[2]);
            }

            // videos/{userId}/...
            if (parts.length >= 2 && "videos".equals(parts[0])) {
                return Long.parseLong(parts[1]);
            }
        } catch (NumberFormatException e) {
            log.warn("无法从路径中提取用户ID: {}", filePath);
        }

        return null;
    }

    /**
     * 获取文件所属的视频ID（从路径中解析）
     * 
     * @param filePath 文件路径
     * @return 视频ID，如果无法解析则返回null
     */
    public static Long extractVideoIdFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        try {
            // 路径格式: videos/{userId}/{videoId}/...
            String[] parts = filePath.split("/");
            if (parts.length >= 3 && "videos".equals(parts[0])) {
                return Long.parseLong(parts[2]);
            }
        } catch (NumberFormatException e) {
            log.warn("无法从路径中提取视频ID: {}", filePath);
        }

        return null;
    }

    /**
     * 获取文件所属的图文内容ID（从路径中解析）
     * 
     * @param filePath 文件路径
     * @return 图文内容ID，如果无法解析则返回null
     */
    public static Long extractPostIdFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        try {
            // 路径格式: images/posts/{userId}/{postId}/...
            String[] parts = filePath.split("/");
            if (parts.length >= 4 && "images".equals(parts[0]) && "posts".equals(parts[1])) {
                return Long.parseLong(parts[3]);
            }
        } catch (NumberFormatException e) {
            log.warn("无法从路径中提取图文内容ID: {}", filePath);
        }

        return null;
    }

    /**
     * 验证文件路径是否合法（防止路径遍历攻击）
     * 
     * @param filePath 文件路径
     * @return true 如果路径合法
     */
    public static boolean isValidPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        // 禁止包含 .. 或 绝对路径
        if (filePath.contains("..") || filePath.startsWith("/") || filePath.contains("\\")) {
            log.warn("检测到非法文件路径: {}", filePath);
            return false;
        }

        // 必须以允许的目录开头
        return filePath.startsWith("images/")
                || filePath.startsWith("videos/")
                || filePath.startsWith("temp/");
    }

    /**
     * 判断路径是否为图片路径
     * 
     * @param filePath 文件路径
     * @return true 如果是图片路径
     */
    public static boolean isImagePath(String filePath) {
        return filePath != null && filePath.startsWith("images/");
    }

    /**
     * 判断路径是否为视频路径
     * 
     * @param filePath 文件路径
     * @return true 如果是视频路径
     */
    public static boolean isVideoPath(String filePath) {
        return filePath != null && filePath.startsWith("videos/");
    }
}
