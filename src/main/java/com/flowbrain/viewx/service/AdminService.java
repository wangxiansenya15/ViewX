package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.vo.VideoReviewVO;

import java.util.List;

/**
 * 管理员服务接口
 */
public interface AdminService {

    /**
     * 获取待审核视频列表
     *
     * @param page 页码
     * @param size 每页大小
     * @return 待审核视频列表
     */
    Result<List<VideoReviewVO>> getPendingVideos(int page, int size);

    /**
     * 获取所有视频列表
     *
     * @param status 视频状态（可选）
     * @param page   页码
     * @param size   每页大小
     * @return 视频列表
     */
    Result<List<VideoReviewVO>> getAllVideos(String status, int page, int size);

    /**
     * 审核通过视频
     *
     * @param videoId 视频ID
     * @return 操作结果
     */
    Result<String> approveVideo(Long videoId);

    /**
     * 拒绝视频
     *
     * @param videoId 视频ID
     * @param reason  拒绝原因
     * @return 操作结果
     */
    Result<String> rejectVideo(Long videoId, String reason);

    /**
     * 删除视频
     *
     * @param videoId 视频ID
     * @return 操作结果
     */
    Result<String> deleteVideo(Long videoId);
}
