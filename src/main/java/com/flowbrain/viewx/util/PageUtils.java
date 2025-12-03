package com.flowbrain.viewx.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * MyBatis-Plus 分页工具类
 */
public class PageUtils {
    /**
     * 创建分页对象
     * @param page  页码（从1开始）
     * @param size 每页显示数量
     */
    public static <T> Page<T> createPage(int page, int size) {
        return new Page<>(page, size);
    }
}
