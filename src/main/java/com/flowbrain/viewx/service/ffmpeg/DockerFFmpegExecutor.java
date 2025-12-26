package com.flowbrain.viewx.service.ffmpeg;

import com.flowbrain.viewx.config.FFmpegPerformanceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.io.File;

/**
 * Docker容器方式执行FFmpeg（性能优化版）
 * 
 * 配置: ffmpeg.executor.type=docker
 * 
 * 性能优化:
 * - 限制并发任务数，防止内存溢出
 * - 限制FFmpeg线程数，降低CPU占用
 * - 低内存模式，减少内存使用
 * - 超时控制，防止任务卡死
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ffmpeg.executor.type", havingValue = "docker", matchIfMissing = true)
public class DockerFFmpegExecutor implements FFmpegExecutor {

    private static final String CONTAINER_NAME = "viewx-ffmpeg";
    private static final String WORKDIR = "/workdir";

    @Autowired
    private FFmpegPerformanceConfig performanceConfig;

    // 信号量，限制并发FFmpeg任务数
    private Semaphore semaphore;

    @PostConstruct
    public void init() {
        semaphore = new Semaphore(performanceConfig.getMaxConcurrentTasks());
        log.info("DockerFFmpegExecutor初始化完成，最大并发任务数: {}, 线程数限制: {}",
                performanceConfig.getMaxConcurrentTasks(),
                performanceConfig.getThreads());
    }

    @Override
    public String execute(List<String> ffmpegArgs) throws Exception {
        // 获取信号量，限制并发
        boolean acquired = semaphore.tryAcquire(performanceConfig.getTimeoutSeconds(), TimeUnit.SECONDS);
        if (!acquired) {
            throw new RuntimeException("FFmpeg任务队列已满，请稍后重试");
        }

        try {
            // 构建完整的docker exec命令
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("exec");
            command.add(CONTAINER_NAME);
            command.add("ffmpeg");

            // 添加性能限制参数
            if (performanceConfig.getThreads() > 0) {
                command.add("-threads");
                command.add(String.valueOf(performanceConfig.getThreads()));
            }

            // 低内存模式
            if (performanceConfig.isLowMemoryMode()) {
                command.add("-preset");
                command.add("ultrafast"); // 最快编码速度，降低内存占用
                command.add("-tune");
                command.add("fastdecode"); // 优化解码速度
            }

            command.addAll(ffmpegArgs);

            log.info("执行Docker FFmpeg命令（性能限制）: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待进程完成，带超时
            boolean finished = process.waitFor(performanceConfig.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("FFmpeg任务超时（" + performanceConfig.getTimeoutSeconds() + "秒）");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg命令执行失败，退出码: {}, 输出: {}", exitCode, output);
                throw new RuntimeException("FFmpeg命令执行失败: " + output);
            }

            log.info("FFmpeg命令执行成功");
            return output.toString();

        } finally {
            // 释放信号量
            semaphore.release();
        }
    }

    @Override
    public String extractFrame(File videoFile, int timestamp, String outputPath) throws Exception {
        String videoFileName = videoFile.getName();
        String thumbnailFileName = videoFileName.replaceFirst("\\.[^.]+$", "_thumb_" + timestamp + ".jpg");
        String containerOutputPath = WORKDIR + "/" + thumbnailFileName;

        List<String> args = new ArrayList<>();
        args.add("-ss");
        args.add(String.valueOf(timestamp));
        args.add("-i");
        args.add(WORKDIR + "/" + videoFileName);
        args.add("-vframes");
        args.add("1");
        args.add("-q:v");
        args.add("2");

        // 限制分辨率，节省内存
        if (performanceConfig.getMaxResolutionWidth() > 0) {
            args.add("-vf");
            args.add(String.format("scale='min(%d,iw)':'min(%d,ih)':force_original_aspect_ratio=decrease",
                    performanceConfig.getMaxResolutionWidth(),
                    performanceConfig.getMaxResolutionHeight()));
        }

        args.add("-y");
        args.add(containerOutputPath);

        execute(args);

        log.info("Docker FFmpeg提取关键帧成功: {}", thumbnailFileName);
        return thumbnailFileName;
    }

    @Override
    public String getExecutorType() {
        return "docker (optimized)";
    }
}
