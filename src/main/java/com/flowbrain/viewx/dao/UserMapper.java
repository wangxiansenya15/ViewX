package com.flowbrain.viewx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowbrain.viewx.pojo.entity.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Insert("INSERT INTO vx_users (id, username, password_encrypted, email,role) VALUES (#{id}, #{username}, #{password}, #{email}, 'USER')")
    int insertUser(User user);

    @Update("UPDATE vx_users SET is_deleted=true, deleted_at=NOW() WHERE id=#{id}")
    boolean deleteUserById(@Param("id") Long id);

    @Select("SELECT u.id, u.username, u.password_encrypted as password, u.email, u.phone, u.nickname, u.role, " +
            "u.enabled, u.account_non_expired, u.account_non_locked, u.credentials_non_expired, " +
            "u.created_at as registerTime, u.updated_at as updateTime " +
            "FROM vx_users u WHERE u.id=#{id} AND u.is_deleted=false")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "details", column = "id", one = @One(select = "com.flowbrain.viewx.dao.UserDetailMapper.selectByUserId"))
    })
    User selectUserWithDetailsById(Long id);

    @Select("SELECT *, created_at as registerTime, updated_at as updateTime FROM vx_users WHERE id=#{id} AND is_deleted=false")
    User selectUserById(Long id);

    @Select("SELECT id, username, email, role, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at as registerTime, updated_at as updateTime FROM vx_users WHERE is_deleted=false")
    List<User> selectAllUsers();

    @Update("UPDATE vx_users SET enabled=#{enabled}, account_non_expired=#{accountNonExpired}, account_non_locked=#{accountNonLocked}, credentials_non_expired=#{credentialsNonExpired} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id,
            @Param("enabled") boolean enabled,
            @Param("accountNonExpired") boolean accountNonExpired,
            @Param("accountNonLocked") boolean accountNonLocked,
            @Param("credentialsNonExpired") boolean credentialsNonExpired);

    @Select("select id from vx_users where username= #{username}")
    long selectIdByUsername(String username);

    @Insert("INSERT INTO vx_user_details (user_id) VALUES (#{user_id})")
    int insertUserDetail(Long id);

    /**
     * 更新用户密码
     * 用于密码重置功能，直接更新用户的加密密码
     * 
     * @param userId          用户ID
     * @param encodedPassword 已加密的新密码
     * @return 影响的行数
     */
    @Update("UPDATE vx_users SET password_encrypted = #{encodedPassword}, updated_at = NOW() WHERE id=#{userId}")
    int updateUserPassword(@Param("userId") Long userId, @Param("encodedPassword") String encodedPassword);

    /**
     * 搜索用户（通过用户名或昵称）
     * 使用 ILIKE 进行不区分大小写的模糊搜索
     * 
     * @param keyword 搜索关键词
     * @param offset  偏移量
     * @param limit   限制数量
     * @return 用户列表
     */
    @Select("SELECT u.id, u.username, u.nickname, ud.avatar_url AS avatar " +
            "FROM vx_users u " +
            "LEFT JOIN vx_user_details ud ON u.id = ud.user_id " +
            "WHERE u.is_deleted = false " +
            "AND (u.username ILIKE CONCAT('%', #{keyword}, '%') OR u.nickname ILIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY " +
            "  CASE " +
            "    WHEN u.username = #{keyword} THEN 1 " +
            "    WHEN u.nickname = #{keyword} THEN 2 " +
            "    WHEN u.username ILIKE CONCAT(#{keyword}, '%') THEN 3 " +
            "    WHEN u.nickname ILIKE CONCAT(#{keyword}, '%') THEN 4 " +
            "    ELSE 5 " +
            "  END, " +
            "  u.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "username", column = "username"),
            @Result(property = "nickname", column = "nickname"),
            @Result(property = "avatar", column = "avatar")
    })
    List<com.flowbrain.viewx.pojo.vo.UserSummaryVO> searchUsers(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit);
}
