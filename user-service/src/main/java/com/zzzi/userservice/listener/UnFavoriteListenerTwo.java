package com.zzzi.userservice.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.userservice.entity.UserDO;
import com.zzzi.userservice.mapper.UserMapper;
import com.zzzi.common.utils.UpdateUserInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;


/**
 * @author zzzi
 * @date 2024/3/29 16:38
 * 在这里异步的更新用户的基本信息
 */
@Service
@Slf4j
public class UnFavoriteListenerTwo {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UpdateUserInfoUtils updateUserInfoUtils;
    @Autowired
    private Gson gson;
    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * @author zzzi
     * @date 2024/3/29 16:49
     * 用户关注在这里更新双方的用户信息
     */
    @RabbitListener(queues = {RabbitMQKeys.UN_FAVORITE_USER})
    @Transactional
    public void listenToUnFavorite(@Payload long[] ids) {
        log.info("第二个消费者监听到用户取消点赞操作，更新用户信息");
        //两个用户都更新
        UserDO userA = userMapper.selectById(ids[0]);

        //A的点赞数-1
        Integer favoriteCount = userA.getFavoriteCount();
        LambdaQueryWrapper<UserDO> queryWrapperA = new LambdaQueryWrapper<>();
        //加上乐观锁，判断当前更新时查到的数据是否被其他线程更新过了
        queryWrapperA.eq(UserDO::getFavoriteCount, favoriteCount);
        userA.setFavoriteCount(favoriteCount - 1);
        int updateA = userMapper.update(userA, queryWrapperA);
        if (updateA != 1) {
            //手动实现CAS算法
            UnFavoriteListenerOne UnFavoriteListener = (UnFavoriteListenerOne) AopContext.currentProxy();
            UnFavoriteListener.listenToUnFavorite(ids);
        }

        //B的获赞总数-1
        UserDO userB = userMapper.selectById(ids[1]);
        Long totalFavorited = userB.getTotalFavorited();
        LambdaQueryWrapper<UserDO> queryWrapperB = new LambdaQueryWrapper<>();
        //加上乐观锁，判断当前更新时查到的数据是否被其他线程更新过了
        queryWrapperB.eq(UserDO::getTotalFavorited, totalFavorited);
        userB.setTotalFavorited(totalFavorited - 1);
        /**@author zzzi
         * @date 2024/4/14 17:20
         * 获赞总数小于1W时从大V列表中删除
         */
        if (totalFavorited - 1 < 10000) {
            updateUserInfoUtils.deleteHotUserFormCache(userB.getUserId());
        }
        int updateB = userMapper.update(userB, queryWrapperB);
        if (updateB != 1) {
            //手动实现CAS算法
            UnFavoriteListenerOne UnFavoriteListener = (UnFavoriteListenerOne) AopContext.currentProxy();
            UnFavoriteListener.listenToUnFavorite(ids);
        }

        //更新两个用户的缓存信息
        String userAJson = gson.toJson(userA);
        String userBJson = gson.toJson(userB);

        updateUserInfoUtils.updateUserInfoCache(userA.getUserId(), userAJson);
        updateUserInfoUtils.updateUserInfoCache(userB.getUserId(), userBJson);
    }


}
