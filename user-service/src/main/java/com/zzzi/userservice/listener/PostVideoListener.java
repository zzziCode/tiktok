package com.zzzi.userservice.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.UserException;
import com.zzzi.common.utils.MD5Utils;
import com.zzzi.userservice.entity.UserDO;
import com.zzzi.userservice.mapper.UserMapper;
import com.zzzi.userservice.utils.UpdateUserInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * @author zzzi
 * @date 2024/3/27 15:44
 * 在这里监听投稿的操作，便于更新用户表中的作品数
 * 并且更新缓存
 */
@Service
@Slf4j
public class PostVideoListener {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UpdateUserInfoUtils updateUserInfoUtils;


    /**
     * @author zzzi
     * @date 2024/3/27 16:26
     * 在这里需要修改用户表中的作品数
     * 以及修改用户缓存中的数据
     * <p>
     * 这里需要互斥锁，因为修改缓存需要互斥
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "direct.post_video"),
                    exchange = @Exchange(name = RabbitMQKeys.EXCHANGE_NAME, type = ExchangeTypes.DIRECT),
                    key = {RabbitMQKeys.VIDEO_POST}
            )
    )
    @Transactional
    public void listenToPostVideo(Long authorId) {
        log.info("监听到用户id为：{}", authorId);
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        //查询得到用户原有信息
        queryWrapper.eq(UserDO::getUserId, authorId);
        UserDO userDO = userMapper.selectOne(queryWrapper);
        Integer workCount = userDO.getWorkCount();

        //更新用户作品信息
        userDO.setWorkCount(workCount + 1);
        //更新用户表中的作品数
        userMapper.updateById(userDO);

        Gson gson = new Gson();
        String userDOJson = gson.toJson(userDO);
        //更新用户的缓存
        updateUserInfoUtils.updateUserInfoCache(authorId, userDOJson);
    }

}
