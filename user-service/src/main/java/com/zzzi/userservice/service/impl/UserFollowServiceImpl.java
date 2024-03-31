package com.zzzi.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.constant.RedisDefaultValue;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.FollowException;
import com.zzzi.common.exception.UserInfoException;
import com.zzzi.common.result.UserVO;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.common.utils.RandomUtils;
import com.zzzi.userservice.entity.UserFollowDO;
import com.zzzi.userservice.mapper.UserFollowMapper;
import com.zzzi.userservice.service.UserFollowService;
import com.zzzi.userservice.service.UserService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollowDO> implements UserFollowService {

    @Autowired
    private UserFollowMapper userFollowMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private UserService userService;
    @Autowired
    private RandomUtils randomUtils;

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

        /**@author zzzi
         * @date 2024/3/31 15:47
         * 先更新数据库再删除缓存
         */
        //已经取消关注或者取消关注失败都需要回滚
        int delete = userFollowMapper.delete(queryWrapper);
        if (delete != 1) {
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

        int insert = userFollowMapper.insert(userFollowDO);
        if (insert != 1) {
            //一旦这里出错，说明数据库中已经有了对应的关注关系，此时应该是不能重复关注
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
     * @date 2024/3/29 22:22
     * 先从缓存中获取当前用户的关注列表，获取不到就缓存重建，期间需要互斥，也就是加双重检查锁
     */
    @Override
    public List<UserVO> getFollowList(String user_id, String token) {
        String userIdByToken = JwtUtils.getUserIdByToken(token).toString();
        if (!user_id.equals(userIdByToken)) {
            throw new UserInfoException("获取用户关注列表失败");
        }
        //获取所有的关注列表
        Set<String> follows = redisTemplate.opsForSet().members(RedisKeys.USER_FOLLOWS_PREFIX + user_id);
        //缓存中获取到了数据，此时尝试返回用户的关注列表
        if (!follows.isEmpty()) {
            return packageUserListVO(follows, user_id);
        } else {//缓存中没有，此时加互斥锁进行缓存重建
            try {
                long currentThreadId = Thread.currentThread().getId();
                Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.USER_FOLLOWS_PREFIX + user_id + "_mutex", currentThreadId + "");
                //加互斥锁没加上
                if (!absent) {
                    Thread.sleep(50);
                    getFollowList(user_id, token);
                }
                //在这里就是加上了互斥锁，此时二次判断
                follows = redisTemplate.opsForSet().members(RedisKeys.USER_FOLLOWS_PREFIX + user_id);
                //缓存中获取到了数据，此时尝试返回用户的关注列表
                if (!follows.isEmpty()) {
                    return packageUserListVO(follows, user_id);
                }
                //在这里就是缓存中真的没有，此时需要缓存重建
                return rebuildUserFollowsCache(user_id);
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new UserInfoException("获取用户关注列表失败");
            } finally {
                /**@author zzzi
                 * @date 2024/3/31 16:55
                 * 是加锁的线程才删除锁
                 */
                String currentThreadId = Thread.currentThread().getId() + "";
                String threadId = redisTemplate.opsForValue().get(RedisKeys.USER_FOLLOWS_PREFIX + user_id + "_mutex");
                //是当前加锁的线程，此时才删除锁
                if (currentThreadId.equals(threadId)) {
                    redisTemplate.delete(RedisKeys.USER_FOLLOWS_PREFIX + user_id + "_mutex");
                }
            }
        }
    }

    /**
     * @author zzzi
     * @date 2024/3/31 16:41
     * 重建用户关注列表的缓存
     */
    private List<UserVO> rebuildUserFollowsCache(String user_id) {
        //查询用户关注表中被关注用户的id
        LambdaQueryWrapper<UserFollowDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFollowDO::getFollowerId, user_id).select(UserFollowDO::getFollowedId);
        List<UserFollowDO> followDOList = userFollowMapper.selectList(queryWrapper);
        Set<String> follows = new HashSet<>();
        if (followDOList == null || followDOList.isEmpty()) {
            follows.add(RedisDefaultValue.REDIS_DEFAULT_VALUE);
            /**@author zzzi
             * @date 2024/3/31 16:43
             * 缓存重建，存储默认值，因为当前用户没有关注
             * 默认值五分钟过期
             */
            redisTemplate.opsForSet().add(RedisKeys.USER_FOLLOWS_PREFIX + user_id, RedisDefaultValue.REDIS_DEFAULT_VALUE);
            redisTemplate.expire(RedisKeys.USER_FOLLOWS_PREFIX + user_id, 5, TimeUnit.MINUTES);
        } else {//数据库中查询到了数据，此时将这些数据保存到缓存中，之后返回真实的数据
            /**@author zzzi
             * @date 2024/3/31 17:03
             * 先删除可能存在的默认值
             * 也就是清空缓存
             */
            redisTemplate.delete(RedisKeys.USER_FOLLOWS_PREFIX + user_id);
            for (UserFollowDO userFollowDO : followDOList) {
                String followedId = userFollowDO.getFollowedId().toString();
                follows.add(followedId);
                //将数据保存到缓存中
                redisTemplate.opsForSet().add(RedisKeys.USER_FOLLOWS_PREFIX + user_id, followedId);
            }
        }
        //最后将用户关注列表打包返回
        return packageUserListVO(follows, user_id);
    }

    /**
     * @author zzzi
     * @date 2024/3/31 16:21
     * 将用户所有关注的id转换成用户基本信息
     */
    private List<UserVO> packageUserListVO(Set<String> follows, String user_id) {
        /**@author zzzi
         * @date 2024/3/31 16:17
         * 防止缓存穿透，存储默认值
         * 此时返回null
         */
        if (follows.contains(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
            return null;
        }
        List<UserVO> userVOList = new ArrayList<>();
        //现在就是用户真的有缓存的关注列表
        for (String follow : follows) {
            /**@author zzzi
             * @date 2024/3/31 16:54
             * 根据当前被关注用户的id获取到当前被关注用户的信息
             */
            UserVO userVO = userService.getUserInfo(follow);
            //获取到的是当前用户的关注用户的信息，所以is_follow肯定是true
            //todo:尝试改进这里的状态，is_follow在粉丝列表和关注列表中分别代表什么
            userVO.setIs_follow(true);
            userVOList.add(userVO);
        }
        //不管如何，当前用户的信息被正常操作了，所以用户的token有效期更新
        Integer userTokenExpireTime = randomUtils.createRandomTime();
        redisTemplate.expire(RedisKeys.USER_TOKEN_PREFIX + user_id, userTokenExpireTime, TimeUnit.MINUTES);
        return userVOList;
    }
}
