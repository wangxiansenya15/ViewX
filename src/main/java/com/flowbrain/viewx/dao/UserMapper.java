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
}
