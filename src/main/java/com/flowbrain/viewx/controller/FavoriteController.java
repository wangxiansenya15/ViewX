package com.flowbrain.viewx.controller;


import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.service.FavoriteService;
import com.flowbrain.viewx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @Description:
 * 用户收藏控制器
 * 提供收藏相关的REST API接口
 *
 * @author Anthropic Claude 4.0
 * @date 2025/05/29
 */

@RestController
@RequestMapping("/user/favorites")
@Slf4j
public class FavoriteController {
    
    @Autowired
    private FavoriteService favoriteService;
    
    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户实体对象
     * @return 用户实体
     */
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        log.info("当前用户: {}", username);
        return userService.getUserByUsername(username);
    }

    /**
     * 添加商品到收藏夹
     * POST /user/favorite
     * @param requestBody 请求体，包含productId
     * @return 操作结果
     */
    @PostMapping
    public Result<?> addToFavorites(@RequestBody Map<String, Integer> requestBody) {
        User user = getCurrentUser();
        if (user == null) {
            return Result.error(Result.BAD_REQUEST, "您还未登录，请先登录");
        }

        Integer productId = requestBody.get("productId");
        if (productId == null) {
            return Result.badRequest("商品ID不能为空");
        }

        log.info("用户{}请求收藏商品{}", user.getId(), productId);
        return favoriteService.addToFavorites(user.getId(), productId);
    }

    /**
     * 从收藏夹移除商品
     * DELETE /user/favorite/{productId}
     * @param productId 商品ID
     * @return 操作结果
     */
    @DeleteMapping("/{productId}")
    public Result<?> removeFromFavorites(@PathVariable Integer productId) {
        User user = getCurrentUser();
        if (user == null) {
            return Result.error(Result.BAD_REQUEST, "您还未登录，请先登录");
        }

        if (productId == null) {
            return Result.badRequest("商品ID不能为空");
        }

        log.info("用户{}请求取消收藏商品{}", user.getId(), productId);
        return favoriteService.removeFromFavorites(user.getId(), productId);
    }

    /**
     * 获取用户收藏列表
     * GET /user/favorite
     * @return 收藏列表
     */
    @GetMapping
    public Result<?> getFavoritesList() {
        User user = getCurrentUser();
        if (user == null) {
            return Result.error(Result.BAD_REQUEST, "您还未登录，请先登录");
        }

        log.info("用户{}请求获取收藏列表", user.getId());
        return favoriteService.getFavoritesList(user.getId());
    }

    /**
     * 检查商品是否已被收藏
     * GET /user/favorite/check/{productId}
     * @param productId 商品ID
     * @return 是否已收藏
     */
    @GetMapping("/check/{productId}")
    public Result<?> checkFavoriteStatus(@PathVariable Integer productId) {
        User user = getCurrentUser();
        if (user == null) {
            return Result.error(Result.BAD_REQUEST, "您还未登录，请先登录");
        }

        if (productId == null) {
            return Result.badRequest("商品ID不能为空");
        }

        log.info("用户{}检查商品{}的收藏状态", user.getId(), productId);
        return favoriteService.isFavorited(user.getId(), productId);
    }

    /**
     * 清空收藏夹
     * DELETE /user/favorite
     * @return 操作结果
     */
    @DeleteMapping
    public Result<?> clearAllFavorites() {
        User user = getCurrentUser();
        if (user == null) {
            return Result.error(Result.BAD_REQUEST, "您还未登录，请先登录");
        }

        log.info("用户{}请求清空收藏夹", user.getId());
        return favoriteService.clearAllFavorites(user.getId());
    }

    /**
     * 获取收藏总数
     * GET /user/favorite/count
     * @return 收藏总数
     */
    @GetMapping("/count")
    public Result<?> getFavoriteCount() {
        User user = getCurrentUser();
        if (user == null) {
            return Result.error(Result.BAD_REQUEST, "您还未登录，请先登录");
        }

        log.info("用户{}请求获取收藏总数", user.getId());
        return favoriteService.getFavoriteCount(user.getId());
    }
}