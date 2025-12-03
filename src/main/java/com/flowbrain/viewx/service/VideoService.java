package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.VideoCreateDTO;
import com.flowbrain.viewx.pojo.dto.VideoUpdateDTO;
import com.flowbrain.viewx.pojo.vo.VideoDetailVO;
import org.springframework.web.multipart.MultipartFile;

public interface VideoService {

    /**
     * Get video details by ID
     * @param videoId Video ID
     * @param userId Current user ID (optional, for interaction status)
     * @return Video details
     */
    Result<VideoDetailVO> getVideoDetail(Long videoId, Long userId);

    /**
     * Create a new video
     * @param userId Uploader ID
     * @param videoCreateDTO Video creation data
     * @return Created video ID
     */
    Result<Long> createVideo(Long userId, VideoCreateDTO videoCreateDTO);

    /**
     * Update an existing video
     * @param userId Current user ID (must be the uploader)
     * @param videoId Video ID
     * @param videoUpdateDTO Video update data
     * @return Success message
     */
    Result<String> updateVideo(Long userId, Long videoId, VideoUpdateDTO videoUpdateDTO);

    /**
     * Upload a video file
     * @param file Video file
     * @return File URL
     */
    Result<String> uploadVideoFile(MultipartFile file);

    /**
     * Upload a cover image
     * @param file Image file
     * @return File URL
     */
    Result<String> uploadCoverImage(MultipartFile file);
}
