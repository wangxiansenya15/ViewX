package com.flowbrain.viewx.dao;

import com.flowbrain.viewx.pojo.entity.ContactMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface ContactMsgMapper {
    @Insert("INSERT INTO contact_messages (username, email, problem, message) " +
            "VALUES (#{username}, #{email}, #{problem}, #{message})")
    int insertContactMsg(ContactMessage contactMsg);
}
