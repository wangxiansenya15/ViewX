package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.service.VideoProcessingService;
import com.flowbrain.viewx.service.ffmpeg.FFmpegExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * 视频处理服务实现
 * 
 * 支持两种FFmpeg部署方式（通过配置切换）：
 * 1. Docker容器部署: ffmpeg.executor.type=docker (默认)
 * 2. 原生Linux部署: ffmpeg.executor.type=native
 */
@Service
@Slf4j
public class VideoProcessingServiceImpl implements VideoProcessingService {

    @Autowired
    private FFmpegExecutor ffmpegExecutor;

    /**
     * 从封面图生成缩略图（压缩版本）
     * 
     * @param coverFile 封面图文件
     * @return 缩略图的字节数组
     */
    @Override
    public byte[] generateThumbnailFromCover(MultipartFile coverFile) {
        try {
            // 读取原始图片
            BufferedImage originalImage = ImageIO.read(coverFile.getInputStream());

            if (originalImage == null) {
                throw new IOException("无法读取图片文件");
            }

            // 计算缩略图尺寸（保持16:9比例）
            int targetWidth = 320;
            int targetHeight = 180;

            // 创建缩略图
            BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumbnail.createGraphics();

            // 设置高质量渲染
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制缩放后的图片
            g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g.dispose();

            // 转换为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", baos);

            log.info("成功生成缩略图，原始尺寸: {}x{}, 缩略图尺寸: {}x{}",
                    originalImage.getWidth(), originalImage.getHeight(),
                    targetWidth, targetHeight);

            return baos.toByteArray();

        } catch (IOException e) {
            log.error("生成缩略图失败", e);
            throw new RuntimeException("生成缩略图失败: " + e.getMessage());
        }
    }

    @Override
    public String generateThumbnail(File videoFile, int timestamp) {
        try {
            log.info("使用{}方式提取视频关键帧: {}, 时间戳: {}秒",
                    ffmpegExecutor.getExecutorType(), videoFile.getName(), timestamp);

            // 使用策略模式执行FFmpeg命令
            String outputPath = videoFile.getParent();
            String thumbnailFileName = ffmpegExecutor.extractFrame(videoFile, timestamp, outputPath);

            log.info("成功生成视频缩略图: {}", thumbnailFileName);
            return thumbnailFileName;

        } catch (Exception e) {
            log.error("生成视频缩略图失败", e);
            throw new RuntimeException("生成视频缩略图失败: " + e.getMessage());
        }
    }

    @Override
    public String generatePreview(File videoFile, int duration) {
        // TODO: 实现预览片段生成（可选功能）
        log.warn("预览片段生成功能暂未实现");
        return null;
    }

    @Override
    public int getVideoDuration(File videoFile) {
        return 0;
    }
}
