package com.zzzi.userservice.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.FollowException;
import com.zzzi.userservice.entity.UserDO;
import com.zzzi.userservice.entity.UserFollowDO;
import com.zzzi.userservice.mapper.UserMapper;
import com.zzzi.common.utils.UpdateUserInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author zzzi
 * @date 2024/3/29 16:38
 * 在这里异步的更新用户的基本信息
 */
@Service
@Slf4j
public class FollowListener {
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
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "direct.follow"),
                    exchange = @Exchange(name = RabbitMQKeys.FOLLOW_EXCHANGE, type = ExchangeTypes.DIRECT),
                    key = {RabbitMQKeys.FOLLOW_KEY}
            )
    )
    @Transactional
    public void listenToFollow(String userFollowDOJson) {
        log.info("监听到用户关注操作");
        //将接收到的实体转换成实体类
        UserFollowDO userFollowDO = gson.fromJson(userFollowDOJson, UserFollowDO.class);

        //得到关注者和被关注者的id
        Long followerId = userFollowDO.getFollowerId();
        Long followedId = userFollowDO.getFollowedId();

        //得到关注者的信息
        UserDO follower = userMapper.selectById(followerId);

        //更新关注者的关注数
        Integer followCount = follower.getFollowCount();
        LambdaQueryWrapper<UserDO> followWrapper = new LambdaQueryWrapper<>();
        //加上乐观锁
        followWrapper.eq(UserDO::getFollowCount, followCount);
        follower.setFollowCount(followCount + 1);
        //更新关注者的关注数量
        int updateFollower = userMapper.update(follower, followWrapper);
        if (updateFollower != 1) {
            //手动实现CAS算法
            FollowListener followListener = (FollowListener) AopContext.currentProxy();
            followListener.listenToFollow(userFollowDOJson);
        }
        //更新被关注者的粉丝数
        //得到被关注者的信息
        UserDO followed = userMapper.selectById(followedId);
        Integer followerCount = followed.getFollowerCount();
        LambdaQueryWrapper<UserDO> followedWrapper = new LambdaQueryWrapper<>();
        followedWrapper.eq(UserDO::getFollowerCount, followerCount);
        followed.setFollowerCount(followerCount + 1);
        /**@author zzzi
         * @date 2024/4/14 17:21
         * 粉丝关注量超过1W就将用户添加到大V列表中，认为是大V
         */
        if (followerCount + 1 >= 10000) {
            redisTemplate.opsForSet().add(RedisKeys.USER_HOT, followed.getUserId().toString());
        }
        int updateFollowed = userMapper.update(followed, followedWrapper);
        if (updateFollowed != 1) {
            throw new FollowException("用户关注失败");
        }

        //调用方法更新用户缓存
        String followerJson = gson.toJson(follower);
        String followedJson = gson.toJson(followed);

        updateUserInfoUtils.updateUserInfoCache(followerId, followerJson);
        updateUserInfoUtils.updateUserInfoCache(followedId, followedJson);
    }
}
