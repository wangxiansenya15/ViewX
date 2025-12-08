package com.flowbrain.viewx.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 视频处理服务
 * 用于生成缩略图、预览片段等
 */
public interface VideoProcessingService {

    /**
     * 从视频文件生成缩略图（需要FFmpeg）
     * 
     * @param videoFile 视频文件
     * @param timestamp 截取时间点（秒），默认为1秒
     * @return 缩略图URL
     */
    String generateThumbnail(File videoFile, int timestamp);

    /**
     * 从封面图生成缩略图（压缩版本）
     * 
     * @param coverFile 封面图文件
     * @return 缩略图的字节数组
     */
    byte[] generateThumbnailFromCover(MultipartFile coverFile);

    /**
     * 生成预览片段（前N秒的低码率视频，需要FFmpeg）
     * 
     * @param videoFile 视频文件
     * @param duration  预览时长（秒），默认10秒
     * @return 预览视频URL
     */
    String generatePreview(File videoFile, int duration);

    /**
     * 获取视频时长（需要FFmpeg）
     * 
     * @param videoFile 视频文件
     * @return 时长（秒）
     */
    int getVideoDuration(File videoFile);
}
