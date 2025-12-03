package com.flowbrain.viewx.dao;


import com.flowbrain.viewx.pojo.entity.Favorite;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: Claude 4.0 Sonnet
 * @date: 2025/05/29 22:05
 *
 * @description:
 * 用户收藏数据访问层
 * 提供收藏相关的数据库操作
 */

@Repository
@Mapper
public interface FavoriteMapper {

    /**
     * 添加商品到收藏夹
     * @param favorite 收藏对象
     * @return 影响行数
     */
    @Insert("INSERT INTO user_favorites (user_id, product_id, created_time) VALUES (#{userId}, #{productId}, NOW())")
    int insertFavorite(Favorite favorite);

    /**
     * 从收藏夹移除商品
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 影响行数
     */
    @Delete("DELETE FROM user_favorites WHERE user_id = #{userId} AND product_id = #{productId}")
    int deleteFavorite(@Param("userId") Long userId, @Param("productId") Integer productId);

    /**
     * 根据用户ID查询收藏列表
     * 联表查询获取商品详细信息
     * @param userId 用户ID
     * @return 收藏列表
     */
    @Select("SELECT f.favorite_id, f.user_id, f.product_id, f.created_time, " +
            "p.product_id as 'product.id', p.name as 'product.name', p.price as 'product.price', " +
            "p.remark as 'product.description', p.category as 'product.category', " +
            "p.count as 'product.stock', p.image_url as 'product.imageUrl', p.rating as 'product.rating' " +
            "FROM user_favorites f " +
            "LEFT JOIN product p ON f.product_id = p.product_id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.created_time DESC")
    @Results({
            @Result(property = "favoriteId", column = "favorite_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "createdTime", column = "created_time"),
            @Result(property = "product.id", column = "product.id"),
            @Result(property = "product.name", column = "product.name"),
            @Result(property = "product.price", column = "product.price"),
            @Result(property = "product.description", column = "product.description"),
            @Result(property = "product.category", column = "product.category"),
            @Result(property = "product.stock", column = "product.stock"),
            @Result(property = "product.imageUrl", column = "product.imageUrl"),
            @Result(property = "product.rating", column = "product.rating")
    })
    List<Favorite> selectFavoritesByUserId(Long userId);

    /**
     * 检查用户是否已收藏某商品
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 收藏记录数量（0表示未收藏，1表示已收藏）
     */
    @Select("SELECT COUNT(*) FROM user_favorites WHERE user_id = #{userId} AND product_id = #{productId}")
    int checkFavoriteExists(@Param("userId") Long userId, @Param("productId") Integer productId);

    /**
     * 根据用户ID删除所有收藏
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM user_favorites WHERE user_id = #{userId}")
    int deleteAllFavoritesByUserId(Long userId);

    /**
     * 获取用户收藏总数
     * @param userId 用户ID
     * @return 收藏总数
     */
    @Select("SELECT COUNT(*) FROM user_favorites WHERE user_id = #{userId}")
    int getFavoriteCountByUserId(Long userId);
}