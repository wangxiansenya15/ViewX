package com.flowbrain.viewx.service.ffmpeg;

import java.io.File;
import java.util.List;

/**
 * FFmpeg执行策略接口
 * 
 * 支持不同的FFmpeg部署方式：
 * - Docker容器部署
 * - 原生Linux部署
 */
public interface FFmpegExecutor {

    /**
     * 执行FFmpeg命令
     * 
     * @param ffmpegArgs FFmpeg参数（不包含ffmpeg命令本身）
     * @return 命令执行输出
     * @throws Exception 执行失败时抛出异常
     */
    String execute(List<String> ffmpegArgs) throws Exception;

    /**
     * 从视频提取关键帧
     * 
     * @param videoFile  视频文件
     * @param timestamp  时间戳（秒）
     * @param outputPath 输出路径
     * @return 输出文件名
     * @throws Exception 执行失败时抛出异常
     */
    String extractFrame(File videoFile, int timestamp, String outputPath) throws Exception;

    /**
     * 获取执行器类型
     * 
     * @return 执行器类型名称
     */
    String getExecutorType();
}
