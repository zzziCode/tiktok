package com.zzzi.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzzi.common.result.MessageVO;
import com.zzzi.userservice.entity.MessageDO;

import java.util.List;

public interface MessageService extends IService<MessageDO> {
    void messageAction(String token, String to_user_id, String content);

    List<MessageVO> getMessageList(String token, String to_user_id);

}
