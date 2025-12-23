package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.vo.VideoReviewVO;
import com.flowbrain.viewx.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 * 处理视频审核、内容管理等管理员功能
 */
@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 获取待审核视频列表
     * GET /admin/videos/pending
     */
    @GetMapping("/videos/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REVIEWER')")
    public Result<List<VideoReviewVO>> getPendingVideos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("获取待审核视频列表，页码: {}, 大小: {}", page, size);
        return adminService.getPendingVideos(page, size);
    }

    /**
     * 获取所有视频列表（包含所有状态）
     * GET /admin/videos
     */
    @GetMapping("/videos")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REVIEWER')")
    public Result<List<VideoReviewVO>> getAllVideos(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("获取视频列表，状态: {}, 页码: {}, 大小: {}", status, page, size);
        return adminService.getAllVideos(status, page, size);
    }

    /**
     * 审核通过视频
     * POST /admin/videos/{id}/approve
     */
    @PostMapping("/videos/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REVIEWER')")
    public Result<String> approveVideo(@PathVariable Long id) {
        log.info("审核通过视频，视频ID: {}", id);
        return adminService.approveVideo(id);
    }

    /**
     * 拒绝视频
     * POST /admin/videos/{id}/reject
     */
    @PostMapping("/videos/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REVIEWER')")
    public Result<String> rejectVideo(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "内容不符合平台规范";
        log.info("拒绝视频，视频ID: {}, 原因: {}", id, reason);
        return adminService.rejectVideo(id, reason);
    }

    /**
     * 删除视频（管理员权限）
     * DELETE /admin/videos/{id}
     */
    @DeleteMapping("/videos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<String> deleteVideo(@PathVariable Long id) {
        log.info("管理员删除视频，视频ID: {}", id);
        return adminService.deleteVideo(id);
    }

    /**
     * 获取仪表盘统计数据
     * GET /admin/dashboard/stats
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REVIEWER')")
    public Result<com.flowbrain.viewx.pojo.vo.DashboardStatsVO> getDashboardStats() {
        return adminService.getDashboardStats();
    }

    /**
     * 创建用户（管理员权限）
     * POST /admin/user-management
     */
    @PostMapping("/user-management")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Long> createUser(@RequestBody com.flowbrain.viewx.pojo.dto.AdminCreateUserDTO dto) {
        log.info("管理员创建用户，用户名: {}", dto.getUsername());
        return adminService.createUser(dto);
    }

    /**
     * 删除用户（管理员权限）
     * DELETE /admin/user-management/{id}
     */
    @DeleteMapping("/user-management/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<String> deleteUser(@PathVariable Long id) {
        log.info("管理员删除用户，用户ID: {}", id);
        return adminService.deleteUser(id);
    }
}
