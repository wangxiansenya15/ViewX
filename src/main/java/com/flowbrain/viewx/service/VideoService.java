package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.VideoUploadDTO;
import com.flowbrain.viewx.pojo.dto.VideoUpdateDTO;
import com.flowbrain.viewx.pojo.vo.VideoDetailVO;
import org.springframework.web.multipart.MultipartFile;

public interface VideoService {

    /**
     * Get video details by ID
     * 
     * @param videoId Video ID
     * @param userId  Current user ID (optional, for interaction status)
     * @return Video details
     */
    Result<VideoDetailVO> getVideoDetail(Long videoId, Long userId);

    /**
     * Upload video and create video record
     * 上传视频并创建视频记录（合并了文件上传和视频创建）
     * 
     * @param userId    Uploader ID
     * @param videoFile Video file
     * @param dto       Video metadata (title, description, etc.)
     * @return Created video ID
     */
    Result<Long> uploadVideo(Long userId, MultipartFile videoFile, VideoUploadDTO dto);

    /**
     * Update an existing video
     * 
     * @param userId         Current user ID (must be the uploader)
     * @param videoId        Video ID
     * @param videoUpdateDTO Video update data
     * @return Success message
     */
    Result<String> updateVideo(Long userId, Long videoId, VideoUpdateDTO videoUpdateDTO);

    /**
     * Upload a cover image
     * 
     * @param file Image file
     * @return File URL
     */
    Result<String> uploadCoverImage(MultipartFile file);

    /**
     * Delete video
     */
    Result<String> deleteVideo(Long userId, Long videoId);

    /**
     * Get my videos
     */
    Result<java.util.List<com.flowbrain.viewx.pojo.entity.Video>> getMyVideos(Long userId);
}
