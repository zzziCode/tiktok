package com.zzzi.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzzi.common.result.MessageVO;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.common.utils.UpdateTokenUtils;
import com.zzzi.userservice.entity.MessageDO;
import com.zzzi.userservice.mapper.MessageMapper;
import com.zzzi.userservice.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, MessageDO> implements MessageService {
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private UpdateTokenUtils updateTokenUtils;

    /**
     * @author zzzi
     * @date 2024/4/4 14:54
     * 直接将当前两个用户的消息保存到数据库中
     */
    @Override
    @Transactional
    public void messageAction(String token, String to_user_id, String content) {
        log.info("用户发送消息service，用户token为：{}，to_user_id为:{}", token, to_user_id);
        //解析得到消息发送方id
        Long fromUserId = JwtUtils.getUserIdByToken(token);
        Long toUserId = Long.valueOf(to_user_id);
        MessageDO messageDO = new MessageDO();
        messageDO.setFromUserId(fromUserId);
        messageDO.setToUserId(toUserId);
        messageDO.setContent(content);

        //将其插入到数据库中
        int insert = messageMapper.insert(messageDO);
        if (insert != 1) {//插入失败
            throw new RuntimeException("用户发送消息失败");
        }
        //更新发送方的token
        updateTokenUtils.updateTokenExpireTimeUtils(fromUserId.toString());
    }

    @Override
    public List<MessageVO> getMessageList(String token, String to_user_id) {
        log.info("获取用户消息列表，token为：{}，to_user_id为：{}", token, to_user_id);
        List<MessageVO> message_list = new ArrayList<>();
        //解析得到用户的id
        Long fromUserId = JwtUtils.getUserIdByToken(token);
        //从数据库中查询得到所有的消息列表
        /**@author zzzi
         * @date 2024/4/4 16:25
         * 这里获取的是
         */
        LambdaQueryWrapper<MessageDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MessageDO::getFromUserId, fromUserId).eq(MessageDO::getToUserId, to_user_id);

        //将获得的所有MessageDO打包成MessageVO
        List<MessageDO> messageDOList = messageMapper.selectList(queryWrapper);
        for (MessageDO messageDO : messageDOList) {
            MessageVO messageVO = packageMessageVO(messageDO);
            message_list.add(messageVO);
        }
        //更新消息获取方的token
        updateTokenUtils.updateTokenExpireTimeUtils(fromUserId.toString());
        return message_list;
    }

    /**
     * @author zzzi
     * @date 2024/4/4 15:01
     * 打包一个MessageVO
     */
    private MessageVO packageMessageVO(MessageDO messageDO) {
        MessageVO messageVO = new MessageVO();
        messageVO.setId(messageDO.getMessageId());
        messageVO.setContent(messageDO.getContent());
        messageVO.setCreate_time(messageDO.getCreateTime().getTime());

        return messageVO;
    }
}
