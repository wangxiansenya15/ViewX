package com.flowbrain.viewx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowbrain.viewx.pojo.entity.ActionLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActionLogMapper extends BaseMapper<ActionLog> {
    // 继承 BaseMapper 后，自动拥有 insert、update、delete、select 等方法
    // MyBatis-Plus 会自动处理 ID 生成和字段填充
}
