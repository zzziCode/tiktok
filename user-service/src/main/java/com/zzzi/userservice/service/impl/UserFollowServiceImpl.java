package com.zzzi.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.FollowException;
import com.zzzi.common.result.UserVO;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.common.utils.RandomUtils;
import com.zzzi.userservice.entity.UserFollowDO;
import com.zzzi.userservice.mapper.UserFollowMapper;
import com.zzzi.userservice.service.UserFollowService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;

@Service
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollowDO> implements UserFollowService {

    @Autowired
    private UserFollowMapper userFollowMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    /**
     * @author zzzi
     * @date 2024/3/29 16:20
     * 用户关注操作
     * <p>
     * 用户关注表新增记录
     * <p>
     * 当前用户更新：
     * 1. 用户关注缓存，主要新增关注id
     * 2. 异步更新用户信息缓存，主要新增关注数量以及信息表更新
     * 3. 更新token有效期
     * 被关注用户更新：
     * 1. 用户粉丝缓存，主要新增粉丝id
     * 2. 异步更新用户信息缓存，主要新增粉丝数量以及信息表更新
     * 3. 更新token有效期
     */
    @Override
    @Transactional
    public void followAction(String token, Long to_user_id) {

        //解析出当前用户的id
        Long followerId = JwtUtils.getUserIdByToken(token);
        UserFollowDO userFollowDO = new UserFollowDO();

        userFollowDO.setFollowerId(followerId);
        userFollowDO.setFollowedId(to_user_id);

        try {
            //用户关注表中新增一条记录
            userFollowMapper.insert(userFollowDO);
        } catch (Exception e) {
            //一旦这里出错，说明数据库中已经有了对应的关注关系，此时应该是不能重复关注
            log.error(e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new FollowException("关注失败，不能重复关注");
        }

        /**@author zzzi
         * @date 2024/3/29 22:30
         * 关注表和粉丝表缓存的是用户的id
         * 做到缓存隔离，减小缓存更新带来的影响
         * 缓存操作失败会进行回滚
         */
        //当前用户关注缓存新增
        redisTemplate.opsForSet().add(RedisKeys.USER_FOLLOWS_PREFIX + followerId, to_user_id.toString());
        //被关注用户粉丝缓存新增
        redisTemplate.opsForSet().add(RedisKeys.USER_FOLLOWERS_PREFIX + to_user_id, followerId.toString());

        //异步更新两个用户的信息表和个人信息缓存
        //1. 将当前关注记录转换成json进行传递
        Gson gson = new Gson();
        String userFollowDOJson = gson.toJson(userFollowDO);
        rabbitTemplate.convertAndSend(RabbitMQKeys.EXCHANGE_NAME, RabbitMQKeys.FOLLOW_KEY, userFollowDOJson);
    }

    /**
     * @author zzzi
     * @date 2024/3/29 16:20
     * 用户取消关注操作
     * 用户关注表删除记录
     * 当前用户更新：
     * 1. 信息表
     * 2. 用户信息缓存，主要减小关注数量
     * 3. 用户关注缓存，主要删除关注缓存
     * 4. 更新token有效期
     * 被关注用户更新：
     * 1. 信息表
     * 2. 用户信息缓存，主要减小粉丝数量
     * 3. 用户粉丝缓存，主要删除粉丝缓存
     * 4. 更新token有效期
     */
    @Override
    @Transactional
    public void followUnAction(String token, Long to_user_id) {
        //解析出当前用户的id
        Long followerId = JwtUtils.getUserIdByToken(token);
        //删除用户关注表中的记录
        LambdaQueryWrapper<UserFollowDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFollowDO::getFollowerId, followerId).
                eq(UserFollowDO::getFollowedId, to_user_id);

        //已经取消关注或者取消关注失败都需要回滚
        try {
            userFollowMapper.delete(queryWrapper);
        } catch (Exception e) {
            log.error(e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new FollowException("取消关注失败");
        }

        //删除用户的关注缓存
        redisTemplate.delete(RedisKeys.USER_FOLLOWS_PREFIX + followerId);
        //删除被关注用户的粉丝缓存
        redisTemplate.delete(RedisKeys.USER_FOLLOWERS_PREFIX + to_user_id);

        //异步更新两个用户的关注数和粉丝数
        //将当前用户和取消关注的用户id封装起来发送过去
        Gson gson = new Gson();
        UserFollowDO userFollowDO = new UserFollowDO();
        userFollowDO.setFollowerId(followerId);
        userFollowDO.setFollowedId(to_user_id);
        String userFollowDOJson = gson.toJson(userFollowDO);

        rabbitTemplate.convertAndSend(RabbitMQKeys.EXCHANGE_NAME, RabbitMQKeys.UN_FOLLOW_KEY, userFollowDOJson);
    }


    /**
     * @author zzzi
     * @date 2024/3/29 22:22
     * 先从缓存中获取当前用户的关注列表，获取不到就缓存重建，期间需要互斥，也就是加双重检查锁
     */
    @Override
    public List<UserVO> getFollowList(String user_id, String token) {
        return null;
    }
}
