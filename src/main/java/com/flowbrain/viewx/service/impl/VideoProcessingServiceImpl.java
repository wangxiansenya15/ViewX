package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.service.VideoProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * 视频处理服务实现
 * 使用Java内置的图像处理功能（无需FFmpeg）
 */
@Service
@Slf4j
public class VideoProcessingServiceImpl implements VideoProcessingService {

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
            // 生成输出文件名
            String videoFileName = videoFile.getName();
            String thumbnailFileName = videoFileName.replaceFirst("\\.[^.]+$", "_thumb_" + timestamp + ".jpg");
            String outputPath = "/workdir/" + thumbnailFileName;

            // 构建 FFmpeg 命令（在容器内执行）
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", "viewx-ffmpeg",
                    "ffmpeg",
                    "-ss", String.valueOf(timestamp),
                    "-i", "/workdir/" + videoFileName,
                    "-vframes", "1",
                    "-q:v", "2",
                    "-y",
                    outputPath);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出日志
            StringBuilder output = new StringBuilder();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg 截图失败，退出码: {}, 输出: {}", exitCode, output);
                throw new RuntimeException("视频截图失败");
            }

            log.info("成功生成视频缩略图: {}", thumbnailFileName);
            return thumbnailFileName;

        } catch (Exception e) {
            log.error("生成视频缩略图失败", e);
            throw new RuntimeException("生成视频缩略图失败: " + e.getMessage());
        }
    }

    @Override
    public String generatePreview(File videoFile, int duration) {
        try {
            // 生成输出文件名
            String videoFileName = videoFile.getName();
            String previewFileName = videoFileName.replaceFirst("\\.[^.]+$", "_preview.mp4");
            String outputPath = "/workdir/" + previewFileName;

            // 构建 FFmpeg 命令（生成预览片段，取前 N 秒）
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", "viewx-ffmpeg",
                    "ffmpeg",
                    "-i", "/workdir/" + videoFileName,
                    "-t", String.valueOf(duration),
                    "-c:v", "libx264",
                    "-c:a", "aac",
                    "-b:v", "500k",
                    "-b:a", "128k",
                    "-y",
                    outputPath);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出日志
            StringBuilder output = new StringBuilder();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg 预览生成失败，退出码: {}, 输出: {}", exitCode, output);
                throw new RuntimeException("预览片段生成失败");
            }

            log.info("成功生成预览片段: {}", previewFileName);
            return previewFileName;

        } catch (Exception e) {
            log.error("生成预览片段失败", e);
            throw new RuntimeException("生成预览片段失败: " + e.getMessage());
        }
    }

    @Override
    public int getVideoDuration(File videoFile) {
        try {
            String videoFileName = videoFile.getName();

            // 使用 ffprobe 获取视频时长
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", "viewx-ffmpeg",
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    "/workdir/" + videoFileName);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFprobe 获取时长失败，退出码: {}, 输出: {}", exitCode, output);
                throw new RuntimeException("获取视频时长失败");
            }

            // 解析时长（秒）
            String durationStr = output.toString().trim();
            double durationSeconds = Double.parseDouble(durationStr);
            int duration = (int) Math.ceil(durationSeconds);

            log.info("成功获取视频时长: {} 秒", duration);
            return duration;

        } catch (Exception e) {
            log.error("获取视频时长失败", e);
            throw new RuntimeException("获取视频时长失败: " + e.getMessage());
        }
    }
}
