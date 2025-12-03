package com.flowbrain.viewx.dao;

import com.flowbrain.viewx.pojo.entity.SocialUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SocialUserMapper {

    @Select("SELECT * FROM vx_social_users WHERE provider = #{provider} AND provider_user_id = #{providerUserId}")
    SocialUser selectByProvider(String provider, String providerUserId);

    @Insert("INSERT INTO vx_social_users (id, user_id, provider, provider_user_id, nickname, avatar_url, email, created_at) " +
            "VALUES (#{id}, #{userId}, #{provider}, #{providerUserId}, #{nickname}, #{avatarUrl}, #{email}, NOW())")
    void insert(SocialUser socialUser);
}
