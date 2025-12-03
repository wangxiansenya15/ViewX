package com.flowbrain.viewx.service;


import com.flowbrain.viewx.dao.ContactMsgMapper;
import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.entity.ContactMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author wxs
 * @date 2025/06/04 10:47
 */
@Service
@Slf4j
public class MessageService {
    @Autowired
    private ContactMsgMapper contactMsgMapper;

    /**
     * 插入联系消息
     * 使用事务确保数据一致性，如果操作失败会自动回滚
     * @param contactMsg 联系消息对象
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> insertContactMsg(ContactMessage contactMsg) {
        if(contactMsg == null){
            return Result.error(Result.BAD_REQUEST,"参数错误");
        }
        return contactMsgMapper.insertContactMsg(contactMsg) > 0
                ? Result.success("提交成功")
                : Result.ServerError("提交失败");
    }
}
