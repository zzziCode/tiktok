package com.zzzi.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.constant.RedisDefaultValue;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.FollowException;
import com.zzzi.common.exception.RelationException;
import com.zzzi.common.result.UserVO;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.common.utils.UpdateTokenUtils;
import com.zzzi.userservice.entity.UserFollowDO;
import com.zzzi.userservice.mapper.RelationMapper;
import com.zzzi.userservice.service.RelationService;
import com.zzzi.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
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
@Slf4j
public class RelationServiceImpl extends ServiceImpl<RelationMapper, UserFollowDO> implements RelationService {

    @Autowired
    private RelationMapper relationMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private UserService userService;
    @Autowired
    private UpdateTokenUtils updateTokenUtils;

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
        log.info("用户取消关注操作service,token为：{}，to_user_id为：{}", token, to_user_id);
        //解析出当前用户的id
        Long followerId = JwtUtils.getUserIdByToken(token);
        if (followerId.equals(to_user_id))
            throw new FollowException("自己不能取消关注自己");
        //删除用户关注表中的记录
        LambdaQueryWrapper<UserFollowDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFollowDO::getFollowerId, followerId).
                eq(UserFollowDO::getFollowedId, to_user_id);

        /**@author zzzi
         * @date 2024/3/31 15:47
         * 先更新数据库再删除缓存
         */
        //已经取消关注或者取消关注失败都需要回滚
        int delete = relationMapper.delete(queryWrapper);
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

        rabbitTemplate.convertAndSend(RabbitMQKeys.FOLLOW_EXCHANGE, RabbitMQKeys.UN_FOLLOW_KEY, userFollowDOJson);
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
        log.info("用户关注操作service,token为：{}，to_user_id为：{}", token, to_user_id);
        //解析出当前用户的id，也就是谁点的关注
        Long followId = JwtUtils.getUserIdByToken(token);
        if (followId.equals(to_user_id))
            throw new FollowException("自己不能关注自己");
        UserFollowDO userFollowDO = new UserFollowDO();

        userFollowDO.setFollowerId(followId);
        userFollowDO.setFollowedId(to_user_id);

        int insert = relationMapper.insert(userFollowDO);
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
        try {
            //当前用户关注缓存新增，新增之前判断有没有默认值，有的话需要删除
            Set<String> follows = redisTemplate.opsForSet().members(RedisKeys.USER_FOLLOWS_PREFIX + followId);
            if (follows.contains(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
                redisTemplate.delete(RedisKeys.USER_FOLLOWS_PREFIX + followId);
            }
            redisTemplate.opsForSet().add(RedisKeys.USER_FOLLOWS_PREFIX + followId, to_user_id.toString());

            //被关注用户粉丝缓存新增，新增之前判断有没有默认值，有的话需要删除
            Set<String> followers = redisTemplate.opsForSet().members(RedisKeys.USER_FOLLOWERS_PREFIX + to_user_id);
            if (followers.contains(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
                redisTemplate.delete(RedisKeys.USER_FOLLOWERS_PREFIX + to_user_id);
            }
            redisTemplate.opsForSet().add(RedisKeys.USER_FOLLOWERS_PREFIX + to_user_id, followId.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new FollowException("用户关注失败");
        }

        //异步更新两个用户的信息表和个人信息缓存
        //1. 将当前关注记录转换成json进行传递
        Gson gson = new Gson();
        String userFollowDOJson = gson.toJson(userFollowDO);
        rabbitTemplate.convertAndSend(RabbitMQKeys.FOLLOW_EXCHANGE, RabbitMQKeys.FOLLOW_KEY, userFollowDOJson);
    }


    /**
     * @author zzzi
     * @date 2024/3/29 22:22
     * 先从缓存中获取当前用户的关注列表，获取不到就缓存重建，期间需要互斥，也就是加双重检查锁
     */
    @Override
    public List<UserVO> getFollowList(String user_id, String token) {
        log.info("获取用户关注列表service,token为：{}，user_id为：{}", token, user_id);
        //id和token是否能对上
        String userIdByToken = JwtUtils.getUserIdByToken(token).toString();
        if (!user_id.equals(userIdByToken)) {
            throw new RelationException("获取用户关注列表失败");
        }
        //获取所有的关注列表
        Set<String> follows = redisTemplate.opsForSet().members(RedisKeys.USER_FOLLOWS_PREFIX + user_id);
        //缓存中获取到了数据，此时尝试返回用户的关注列表
        if (!follows.isEmpty()) {
            return packageFollowListVO(follows, user_id);
        } else {//缓存中没有，此时加互斥锁进行缓存重建
            try {
                long currentThreadId = Thread.currentThread().getId();
                Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.USER_FOLLOWS_PREFIX + user_id + "_mutex", currentThreadId + "");
                //加互斥锁没加上
                if (!absent) {
                    Thread.sleep(50);
                    RelationService relationService = (RelationService) AopContext.currentProxy();
                    relationService.getFollowList(user_id, token);
                }
                //在这里就是加上了互斥锁，此时二次判断
                follows = redisTemplate.opsForSet().members(RedisKeys.USER_FOLLOWS_PREFIX + user_id);
                //缓存中获取到了数据，此时尝试返回用户的关注列表
                if (!follows.isEmpty()) {
                    return packageFollowListVO(follows, user_id);
                }
                //在这里就是缓存中真的没有，此时需要缓存重建
                return rebuildUserFollowsCache(user_id);
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new RelationException("获取用户关注列表失败");
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
     * @date 2024/4/1 12:40
     * 获取粉丝列表，分为
     * 1. 从缓存中获取，获取到直接返回，获取不到执行2
     * 2. 加互斥锁，加上了二次判断，执行1，执行失败转到3
     * 3. 缓存重建返回最终结果
     */
    @Override
    public List<UserVO> getFollowerList(String user_id, String token) {
        log.info("获取用户粉丝列表service,token为：{}，user_id为：{}", token, user_id);
        //判断token是否正确
        String userIdByToken = JwtUtils.getUserIdByToken(token).toString();
        if (!user_id.equals(userIdByToken)) {
            throw new RelationException("获取用户关注列表失败");
        }

        //1. 从缓存中获取
        Set<String> followers = redisTemplate.opsForSet().members(RedisKeys.USER_FOLLOWERS_PREFIX + user_id);
        //关注列表不为空，此时
        if (!followers.isEmpty()) {
            return packageFollowerListVO(followers, user_id, token);
        } else {//缓存中目前没有，此时加互斥锁重建缓存
            try {
                long currentThreadId = Thread.currentThread().getId();
                Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.USER_FOLLOWERS_PREFIX + user_id + "_mutex", currentThreadId + "");
                //加锁失败
                if (!absent) {
                    Thread.sleep(50);
                    RelationService relationService = (RelationService) AopContext.currentProxy();
                    relationService.getFollowerList(user_id, token);
                }
                //到这里就是加锁成功，此时再次尝试从缓存中获取
                followers = redisTemplate.opsForSet().members(RedisKeys.USER_FOLLOWERS_PREFIX + user_id);
                //关注列表不为空，此时
                if (!followers.isEmpty()) {
                    return packageFollowerListVO(followers, user_id, token);
                }
                //到这里说明缓存中真的没有，此时缓存重建
                return rebuildUserFollowersCache(user_id, token);
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new RelationException("获取用户粉丝列表失败");
            } finally {
                //尝试解锁
                String currentThreadId = Thread.currentThread().getId() + "";
                String threadId = redisTemplate.opsForValue().get(RedisKeys.USER_FOLLOWERS_PREFIX + user_id + "_mutex");
                //是当前加锁的线程，此时才删除锁
                if (currentThreadId.equals(threadId)) {
                    redisTemplate.delete(RedisKeys.USER_FOLLOWERS_PREFIX + user_id + "_mutex");
                }
            }
        }
    }


    /**
     * @author zzzi
     * @date 2024/4/1 12:44
     * 好友列表从关注列表和缓存列表中取交集
     * 不再进行缓存
     * 1. 获取关注列表
     * 2. 获取粉丝列表
     * 3. 取交集
     * 4. 针对交集中的每一个好友，保存到List中
     * 5. 返回好友列表
     * todo：尝试返回好友之间最新的一条消息
     */
    @Override
    public List<UserVO> getFriendList(String user_id, String token) {
        log.info("获取用户好友列表service,token为：{}，user_id为：{}", token, user_id);
        //判断token是否正确
        String userIdByToken = JwtUtils.getUserIdByToken(token).toString();
        if (!user_id.equals(userIdByToken)) {
            throw new RelationException("获取用户好友列表失败");
        }
        //1.获取用户关注列表
        List<UserVO> followList = getFollowList(user_id, token);
        //2. 获取用户粉丝列表
        List<UserVO> followerList = getFollowerList(user_id, token);

        //3. 取交集
        List<UserVO> friends = new ArrayList<>();
        //看用户关注的人是否是自己的粉丝，互关才能成为朋友
        for (UserVO follow : followList) {
            if (followerList != null && followerList.contains(follow)) {
                //互关了，也就是我关注了这个粉丝，此时is_follow一定为true
                follow.setIs_follow(true);
                friends.add(follow);
            }
        }
        //不管如何，要更新当前用户的token有效期
        updateTokenUtils.updateTokenExpireTimeUtils(user_id);
        return friends;
    }

    /**
     * @author zzzi
     * @date 2024/3/31 16:41
     * 重建用户关注列表的缓存
     * 也就是获取谁被关注的列表
     */
    private List<UserVO> rebuildUserFollowsCache(String user_id) {
        log.info("重建用户关注列表缓存service，user_id为：{}", user_id);
        //查询用户关注表中被关注用户的id
        LambdaQueryWrapper<UserFollowDO> queryWrapper = new LambdaQueryWrapper<>();
        //FollowerId代表谁点了关注，FollowedId代表谁被关注
        queryWrapper.eq(UserFollowDO::getFollowerId, Long.valueOf(user_id)).
                select(UserFollowDO::getFollowedId);
        List<UserFollowDO> followDOList = relationMapper.selectList(queryWrapper);
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
                //被关注的人的id是啥
                String followedId = userFollowDO.getFollowedId().toString();
                follows.add(followedId);
                //将数据保存到缓存中
                redisTemplate.opsForSet().add(RedisKeys.USER_FOLLOWS_PREFIX + user_id, followedId);
            }
        }
        //最后将用户关注列表打包返回
        return packageFollowListVO(follows, user_id);
    }

    /**
     * @author zzzi
     * @date 2024/4/1 12:58
     * 重建用户粉丝的缓存
     * 也就是获取谁点了关注的列表
     */
    private List<UserVO> rebuildUserFollowersCache(String user_id, String token) {
        log.info("重建用户粉丝列表缓存service,user_id为 :{},token为：{}", user_id, token);
        //先查询所有的粉丝id
        LambdaQueryWrapper<UserFollowDO> queryWrapper = new LambdaQueryWrapper<>();
        //FollowedId代表谁被关注，FollowerId代表谁点了关注
        queryWrapper.eq(UserFollowDO::getFollowedId, Long.valueOf(user_id)).
                select(UserFollowDO::getFollowerId);
        List<UserFollowDO> followerDOList = relationMapper.selectList(queryWrapper);

        //保存所有的粉丝id
        Set<String> followers = new HashSet<>();
        //数据库中也没有查到，说明当前用户没有粉丝
        if (followerDOList == null || followerDOList.isEmpty()) {
            followers.add(RedisDefaultValue.REDIS_DEFAULT_VALUE);

            /**@author zzzi
             * @date 2024/4/1 13:03
             * 保存防止缓存穿透的默认值
             * 并设置5分钟过期
             */
            redisTemplate.opsForValue().set(RedisKeys.USER_FOLLOWERS_PREFIX + user_id, RedisDefaultValue.REDIS_DEFAULT_VALUE);
            redisTemplate.expire(RedisKeys.USER_FOLLOWERS_PREFIX + user_id, 5, TimeUnit.MINUTES);
        } else {//数据库中有，此时需要重建缓存
            //重建缓存之前先删除缓存中可能存储的默认值
            redisTemplate.delete(RedisKeys.USER_FOLLOWERS_PREFIX + user_id);
            for (UserFollowDO userFollowDO : followerDOList) {
                String followerId = userFollowDO.getFollowerId().toString();
                followers.add(followerId);
                //将数据保存到缓存中
                redisTemplate.opsForSet().add(RedisKeys.USER_FOLLOWERS_PREFIX + user_id, followerId);
            }
        }

        //不管怎么样，都需要将获取到的用户粉丝列表进行打包返回
        return packageFollowerListVO(followers, user_id, token);
    }

    /**
     * @author zzzi
     * @date 2024/3/31 16:21
     * 将用户所有关注的id转换成用户基本信息
     */
    private List<UserVO> packageFollowListVO(Set<String> follows, String user_id) {
        log.info("打包用户关注列表缓存service，user_id为：{}", user_id);
        /**@author zzzi
         * @date 2024/3/31 16:17
         * 防止缓存穿透，存储默认值
         * 此时返回null
         */
        if (follows.contains(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
            return null;
        }
        List<UserVO> userFollowVOList = new ArrayList<>();
        //现在就是用户真的有缓存的关注列表
        for (String follow : follows) {
            /**@author zzzi
             * @date 2024/3/31 16:54
             * 根据当前被关注用户的id获取到当前被关注用户的信息
             */
            UserVO userVO = userService.getUserInfo(follow);
            //获取到的是当前用户的关注用户的信息，所以is_follow肯定是true
            //关注列表中，当前用户一定是我关注的，这里肯定为true
            userVO.setIs_follow(true);
            userFollowVOList.add(userVO);
        }
        //不管如何，当前用户的信息被正常操作了，所以用户的token有效期更新
        updateTokenUtils.updateTokenExpireTimeUtils(user_id);
        return userFollowVOList;
    }

    /**
     * @author zzzi
     * @date 2024/4/1 12:49
     * 形成用户粉丝列表
     * 粉丝列表中，只有我也关注了，此时才将true设置为true，所以还要获取用户的关注列表
     */
    private List<UserVO> packageFollowerListVO(Set<String> followers, String user_id, String token) {
        log.info("打包用户粉丝列表缓存service,user_id为：{}，token为：{}", user_id, token);
        //用户粉丝列表中有默认值，说明当前用户还没有粉丝
        if (followers.contains(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
            return null;
        }
        //到这里说明当前用户有粉丝，此时需要形成粉丝列表
        //获取用户的关注列表，从而判断is_follow的状态，这里的关注列表中，is_follow全为true
        List<UserVO> followList = getFollowList(user_id, token);
        List<UserVO> userFollowerVOList = new ArrayList<>();
        for (String follower : followers) {//依次拿到每一个粉丝的id
            UserVO userVO = userService.getUserInfo(follower);
            /**@author zzzi
             * @date 2024/4/1 12:52
             * 我关注的粉丝，将is_follow设置为true
             * 也就是当前粉丝是否出现在我的关注列表中
             * equals和hashCode需要重写，并且只按照id和name判断
             */
            //只有我关注的粉丝，这里才能为true
            if (followList.contains(userVO))
                userVO.setIs_follow(true);
            userFollowerVOList.add(userVO);
        }
        //获取了当前用户的粉丝列表，当前用户token需要更新
        updateTokenUtils.updateTokenExpireTimeUtils(user_id);
        //返回用户的粉丝列表
        return userFollowerVOList;
    }
}
