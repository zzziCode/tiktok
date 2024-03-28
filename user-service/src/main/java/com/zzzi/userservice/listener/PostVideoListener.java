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
    private StringRedisTemplate redisTemplate;


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
        String mutex = MD5Utils.parseStrToMd5L32(userDOJson);
        try {
            //拿到互斥锁
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.MUTEX_LOCK_PREFIX + mutex, "");

            //没拿到互斥锁说明当前用户正在被修改，应该重试
            if (!absent) {
                Thread.sleep(50);
                //调用自己重试
                listenToPostVideo(authorId);
            }

            /**@author zzzi
             * @date 2024/3/28 21:17
             * todo：更新用户缓存尝试直接覆盖，而不是先删除再添加新的
             */
            //更新用户缓存(先删除再加入)
            redisTemplate.delete(RedisKeys.USER_INFO_PREFIX + authorId);

            //加入，并设置三十分钟有效期
            redisTemplate.opsForValue().set(RedisKeys.USER_INFO_PREFIX + authorId, userDOJson, 30, TimeUnit.MINUTES);

            //因为当前有操作，所以用户的token有效期需要更新
            redisTemplate.expire(RedisKeys.USER_TOKEN_PREFIX + authorId, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new UserException("更新用户信息失败");
        } finally {//最后释放互斥锁
            redisTemplate.delete(RedisKeys.MUTEX_LOCK_PREFIX + mutex);
        }
    }
}
