package com.zzzi.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//好友之间的消息列表返回的实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageListVO {
    //状态码，0-成功，其他值-失败
    private Integer status_code;
    //状态信息
    private String status_msg;

    //当前用户的聊天记录
    List<MessageVO> message_list;

    public static MessageListVO success(String status_msg, List<MessageVO> message_list) {
        return new MessageListVO(0, status_msg, message_list);
    }

    public static MessageListVO fail(String status_msg) {
        return new MessageListVO(-1, status_msg, null);
    }


}
