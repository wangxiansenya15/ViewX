package com.flowbrain.viewx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowbrain.viewx.pojo.entity.UserDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface UserDetailMapper extends BaseMapper<UserDetail> {

    @Select("SELECT * FROM vx_user_details WHERE user_id = #{userId}")
    UserDetail selectByUserId(Long userId);
}
