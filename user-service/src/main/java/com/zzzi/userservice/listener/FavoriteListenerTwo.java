package com.zzzi.userservice.listener;

import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.userservice.entity.UserDO;
import com.zzzi.userservice.mapper.UserMapper;
import com.zzzi.common.utils.UpdateUserInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author zzzi
 * @date 2024/3/29 16:38
 * 在这里异步的更新用户的基本信息
 */
@Service
@Slf4j
public class FavoriteListenerTwo {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UpdateUserInfoUtils updateUserInfoUtils;


    /**
     * @author zzzi
     * @date 2024/3/29 16:49
     * 用户关注在这里更新双方的用户信息
     * 更新A的点赞数，B的获赞总数
     */
    @RabbitListener(queues = {RabbitMQKeys.FAVORITE_USER})
    @Transactional
    public void listenToFavorite(@Payload Long[] ids) {
        log.info("第二个消费者监听到用户点赞操作");
        //两个用户都更新
        UserDO userA = userMapper.selectById(ids[0]);
        UserDO userB = userMapper.selectById(ids[1]);

        //A的点赞数+1
        Integer favoriteCount = userA.getFavoriteCount();
        userA.setFavoriteCount(favoriteCount + 1);
        userMapper.updateById(userA);
        //B的获赞总数+1
        Long totalFavorited = userB.getTotalFavorited();
        userB.setTotalFavorited(totalFavorited + 1);
        userMapper.updateById(userB);

        //更新两个用户的缓存信息
        Gson gson = new Gson();
        String userAJson = gson.toJson(userA);
        String userBJson = gson.toJson(userB);

        updateUserInfoUtils.updateUserInfoCache(userA.getUserId(), userAJson);
        updateUserInfoUtils.updateUserInfoCache(userB.getUserId(), userBJson);
    }
}
