package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.dto.ContentUploadDTO;
import com.flowbrain.viewx.pojo.vo.ContentDetailVO;
import com.flowbrain.viewx.pojo.vo.ContentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 内容服务接口 - 支持图片、图片集等多种内容类型
 */
public interface ContentService {

    /**
     * 上传单张图片
     * 
     * @param userId 用户ID
     * @param imageFile 图片文件
     * @param dto 内容元数据
     * @return 内容ID
     */
    Result<Long> uploadImage(Long userId, MultipartFile imageFile, ContentUploadDTO dto);

    /**
     * 上传图片集
     * 
     * @param userId 用户ID
     * @param imageFiles 图片文件列表(2-9张)
     * @param dto 内容元数据
     * @return 内容ID
     */
    Result<Long> uploadImageSet(Long userId, List<MultipartFile> imageFiles, ContentUploadDTO dto);

    /**
     * 获取内容详情
     * 
     * @param contentId 内容ID
     * @param userId 当前用户ID(可选)
     * @return 内容详情
     */
    Result<ContentDetailVO> getContentDetail(Long contentId, Long userId);

    /**
     * 获取用户的内容列表
     * 
     * @param userId 用户ID
     * @param contentType 内容类型筛选(可选: VIDEO, IMAGE, IMAGE_SET)
     * @return 内容列表
     */
    Result<List<ContentVO>> getUserContents(Long userId, String contentType);

    /**
     * 删除内容
     * 
     * @param userId 用户ID
     * @param contentId 内容ID
     * @return 结果
     */
    Result<String> deleteContent(Long userId, Long contentId);
}
