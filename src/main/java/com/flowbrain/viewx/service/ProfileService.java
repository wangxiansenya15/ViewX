package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.dao.ProfileMapper;
import com.flowbrain.viewx.dao.UserDetailMapper;
import com.flowbrain.viewx.dao.UserMapper;
import com.flowbrain.viewx.pojo.dto.UserProfileDTO;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.entity.UserDetail;
import com.flowbrain.viewx.pojo.vo.UserProfileVO;
import com.flowbrain.viewx.service.impl.LocalStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户资料服务
 * 展示 VO/DTO/Entity 的最佳实践
 */
@Service
@Slf4j
public class ProfileService {

    @Autowired
    private ProfileMapper profileMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserDetailMapper userDetailMapper;


    /**
     * 获取用户资料
     * 流程：Entity -> VO
     * 
     * @param userId 用户ID
     * @return 用户资料 VO
     */
    public Result<UserProfileVO> getUserProfile(Long userId) {
        try {
            // 1. 查询 User Entity
            // 使用 UserMapper.selectUserById 以确保与 UserService 行为一致
            User user = userMapper.selectUserById(userId);

            if (user == null) {
                // 尝试使用 MyBatis-Plus 的 selectById 再次查询
                user = userMapper.selectById(userId);
                if (user == null) {
                    return Result.error(404, "用户不存在");
                }
            }

            // 2. 查询 UserDetail Entity
            UserDetail userDetail = userDetailMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserDetail>()
                            .eq("user_id", userId));

            // 将详情设置到 user 对象中，以便 convertToVO 使用
            user.setDetails(userDetail);

            // 3. 将 Entity 转换为 VO
            UserProfileVO vo = convertToVO(user);

            // 4. 补充统计信息
            vo.setFollowersCount(profileMapper.countFollowers(userId));
            vo.setFollowingCount(profileMapper.countFollowing(userId));
            vo.setVideoCount(profileMapper.countVideos(userId));

            log.info("获取用户资料成功，用户ID: {}", userId);
            return Result.success(vo);

        } catch (Exception e) {
            log.error("获取用户资料失败，用户ID: {}", userId, e);
            return Result.error(500, "获取用户资料失败");
        }
    }

    /**
     * 更新用户资料
     * 流程：DTO -> Entity -> 数据库
     * 
     * @param userId 用户ID
     * @param dto    用户资料更新 DTO
     * @return 更新后的用户资料 VO
     */
    @Transactional
    public Result<UserProfileVO> updateUserProfile(Long userId, UserProfileDTO dto, Long currentUserId,
            String currentUserRole) {
        try {
            // 1. 验证用户是否存在
            User existingUser = userMapper.selectById(userId);
            if (existingUser == null) {
                return Result.error(404, "用户不存在");
            }

            // 2. 权限检查：只有用户本人、管理员或超级管理员可以修改
            boolean isOwner = userId.equals(currentUserId);
            boolean isAdmin = "ROLE_ADMIN".equals(currentUserRole) || "ROLE_SUPER_ADMIN".equals(currentUserRole);

            if (!isOwner && !isAdmin) {
                log.warn("权限不足：用户 {} (角色: {}) 尝试修改用户 {} 的资料", currentUserId, currentUserRole, userId);
                return Result.error(403, "权限不足：只有用户本人、管理员或超级管理员可以修改用户资料");
            }

            // 3. 更新 User 表
            if (dto.getNickname() != null)
                existingUser.setNickname(dto.getNickname());
            if (dto.getEmail() != null)
                existingUser.setEmail(dto.getEmail());
            if (dto.getPhone() != null)
                existingUser.setPhone(dto.getPhone());

            userMapper.updateById(existingUser);

            // 4. 更新或创建 UserDetail 表
            if (needsDetailUpdate(dto)) {
                updateUserDetail(userId, dto);
            }

            // 5. 重新查询并返回 VO
            log.info("更新用户资料成功，用户ID: {}，操作者ID: {}", userId, currentUserId);
            return getUserProfile(userId);

        } catch (Exception e) {
            log.error("更新用户资料失败，用户ID: {}", userId, e);
            return Result.error(500, "更新用户资料失败");
        }
    }

    /**
     * Entity -> VO 转换
     */
    private UserProfileVO convertToVO(User user) {
        UserProfileVO vo = new UserProfileVO();

        // 基础字段复制
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setCreatedAt(user.getRegisterTime());

        // 角色转换为友好名称
        if (user.getRole() != null) {
            vo.setRole(user.getRole().getLabel());
        }

        // UserDetail 字段
        if (user.getDetails() != null) {
            UserDetail detail = user.getDetails();
            vo.setAvatarUrl(detail.getAvatar());
            vo.setDescription(detail.getDescription());
            vo.setAge(detail.getAge());
            vo.setGender(detail.getGender());
            vo.setAddress(detail.getAddress());
        }

        return vo;
    }

    /**
     * 判断是否需要更新 UserDetail 表
     */
    private boolean needsDetailUpdate(UserProfileDTO dto) {
        return dto.getDescription() != null
                || dto.getAge() != null
                || dto.getGender() != null
                || dto.getAddress() != null
                || dto.getAvatarUrl() != null;
    }

    /**
     * 更新 UserDetail 表
     */
    private void updateUserDetail(Long userId, UserProfileDTO dto) {
        UserDetail detail = userDetailMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserDetail>()
                        .eq("user_id", userId));

        if (detail == null) {
            detail = new UserDetail();
            detail.setUserId(userId);
        }

        if (dto.getDescription() != null)
            detail.setDescription(dto.getDescription());
        if (dto.getAge() != null)
            detail.setAge(dto.getAge());
        if (dto.getGender() != null)
            detail.setGender(dto.getGender());
        if (dto.getAddress() != null)
            detail.setAddress(dto.getAddress());
        if (dto.getAvatarUrl() != null)
            detail.setAvatar(dto.getAvatarUrl());

        if (detail.getId() == null) {
            userDetailMapper.insert(detail);
        } else {
            userDetailMapper.updateById(detail);
        }
    }

    @Autowired
    private LocalStorageStrategy storageStrategy;

    /**
     * 上传头像
     * 
     * @param userId   用户ID
     * @param file     头像文件
     * @param filename 文件名
     * @return 头像访问URL
     */
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file, String filename)
            throws Exception {
        try {
            // 1. 获取用户当前头像
            UserDetail userDetail = userDetailMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserDetail>()
                            .eq("user_id", userId));

            String oldAvatarUrl = null;
            if (userDetail != null && userDetail.getAvatar() != null) {
                oldAvatarUrl = userDetail.getAvatar();
            }

            // 2. 存储新头像
            String avatarFilename = "avatars/" + filename;
            String storedFilename = storageStrategy.storeFile(file, avatarFilename);
            String newAvatarUrl = storageStrategy.getFileUrl(storedFilename);

            // 3. 更新数据库
            if (userDetail == null) {
                userDetail = new UserDetail();
                userDetail.setUserId(userId);
                userDetail.setAvatar(newAvatarUrl);
                userDetailMapper.insert(userDetail);
            } else {
                userDetail.setAvatar(newAvatarUrl);
                userDetailMapper.updateById(userDetail);
            }

            // 4. 删除旧头像（如果存在且不是默认头像）
            if (oldAvatarUrl != null && !oldAvatarUrl.contains("dicebear.com")) {
                try {
                    storageStrategy.deleteFile(oldAvatarUrl);
                    log.info("已删除旧头像: {}", oldAvatarUrl);
                } catch (Exception e) {
                    log.warn("删除旧头像失败: {}", oldAvatarUrl, e);
                    // 不影响主流程，继续执行
                }
            }

            log.info("用户 {} 头像上传成功: {}", userId, newAvatarUrl);
            return newAvatarUrl;

        } catch (Exception e) {
            log.error("上传头像失败，用户ID: {}", userId, e);
            throw new Exception("上传头像失败: " + e.getMessage());
        }
    }
}
