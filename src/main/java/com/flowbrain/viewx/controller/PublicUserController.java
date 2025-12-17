package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class PublicUserController {

    @Autowired
    private UserService userService;

    /**
     * 搜索用户（通过用户名或昵称）
     * GET /users/search?keyword=xxx
     */
    @GetMapping("/search")
    public Result<List<com.flowbrain.viewx.pojo.vo.UserSummaryVO>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("搜索用户，关键词: {}, 页码: {}, 大小: {}", keyword, page, size);

        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.badRequest("搜索关键词不能为空");
        }

        return userService.searchUsers(keyword.trim(), page, size);
    }
}
