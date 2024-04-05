package com.zzzi.userservice.controller;

import com.zzzi.common.result.CommonVO;
import com.zzzi.common.result.MessageListVO;
import com.zzzi.common.result.MessageVO;
import com.zzzi.userservice.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/douyin/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * @author zzzi
     * @date 2024/4/3 16:02
     * 用户发送消息
     * 目前只实现发送消息，不实现撤回消息
     */
    @PostMapping("/action")
    public CommonVO messageAction(String token, String to_user_id, String action_type, String content) {
        log.info("用户发送消息,token为：{}，to_user_id为：{}", token, to_user_id);
        String status_msg = "";
        //截取真正的token
        if (token.startsWith("login:token:"))
            token = token.substring(12);
        if ("1".equals(action_type)) {
            messageService.messageAction(token, to_user_id, content);
            status_msg = "成功发送消息";
        } else {
            //todo: 撤回消息
        }
        return CommonVO.success(status_msg);
    }


    /**
     * @author zzzi
     * @date 2024/4/3 16:04
     * 获取好友之间的聊天记录
     */
    @GetMapping("/chat")
    public MessageListVO getMessageList(String token, String to_user_id) {
        log.info("获取好友之间的聊天记录,token为：{}，to_user_id为：{}", token, to_user_id);
        //截取真正的token
        if (token.startsWith("login:token:"))
            token = token.substring(12);
        List<MessageVO> message_list = messageService.getMessageList(token, to_user_id);
        if (message_list != null) {
            return MessageListVO.success("成功获取聊天记录", message_list);
        }
        return MessageListVO.fail("获取聊天记录失败");
    }
}
