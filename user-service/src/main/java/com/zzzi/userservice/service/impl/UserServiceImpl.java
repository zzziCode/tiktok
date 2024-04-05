package com.zzzi.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.zzzi.common.constant.RedisDefaultValue;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.UserInfoException;
import com.zzzi.common.utils.*;
import com.zzzi.userservice.dto.UserDTO;
import com.zzzi.userservice.entity.UserDO;
import com.zzzi.common.exception.UserException;
import com.zzzi.userservice.mapper.UserMapper;
import com.zzzi.common.result.UserVO;
import com.zzzi.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RandomUtils randomUtils;
    /**
     * @author zzzi
     * @date 2024/3/27 10:27
     * 注入StringRedisTemplate而不是RedisTemplate是因为Redis中存储的就是String类型的数据
     */
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UpdateTokenUtils updateTokenUtils;

    /**
     * @author zzzi
     * @date 2024/3/26 21:30
     * 用户注册之后返回一个用户的token
     * 由于需要操作数据库，所以需要加上事务
     */
    @Override
    @Transactional
    public UserDTO register(String username, String password) {
        log.info("用户注册service,用户名为：{}，密码为：{}", username, password);
        //正则表达式验证邮箱合法性
        String pattern = "^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$";
        if (!Pattern.matches(pattern, username)) {
            throw new UserException("邮箱格式不正确！");
        }

        /**@author zzzi
         * @date 2024/4/3 15:54
         * 两种密码加密方式
         */
        //1. MD5加盐
        String pwdMD5 = MD5Utils.parseStrToMd5L32(password);

        //2. BCrypt自适应算法，逐渐增加密码生成的迭代次数，使加密和解密时间变长
        //BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(16);
        //String pwdMD5 = encoder.encode(password);

        //尝试往数据库中插入数据，插入失败说明用户名被占用
        UserDO userDO = new UserDO();
        userDO.setUsername(username);
        userDO.setEmail(username);
        userDO.setPassword(pwdMD5);
        int insert = userMapper.insert(userDO);
        //插入失败
        if (insert != 1) {
            throw new UserException("用户名被占用，请重新输入用户名");
        }

        Long userId = userDO.getUserId();
        String token = JwtUtils.createToken(userId, username);
        /**@author zzzi
         * @date 2024/3/31 19:27
         * 将用户的token保存到redis中
         * 之后系统自动登录
         * 到这里说明数据库插入成功，token可以正常生成
         */
        Integer userTokenExpireTime = randomUtils.createRandomTime();
        redisTemplate.opsForValue().set(RedisKeys.USER_TOKEN_PREFIX + userId, token, userTokenExpireTime, TimeUnit.MINUTES);
        log.warn("注册生成的token为：{}", token);
        //返回封装好的对象
        return new UserDTO(userDO, "login:token:" + token);
    }

    /**
     * @author zzzi
     * @date 2024/3/26 21:28
     * 判断用户是否存在，存在将用户信息缓存到redis中
     * 用户不能重复登录，判断依据是什么：用户token是否存在
     */
    @Override
    public UserDTO login(String username, String password) {
        log.info("用户登录service,用户名为：{}，密码为：{}", username, password);
        UserDO userDO = getUserDOByPasswordAndUserName(username, password);
        //没有该用户，用户名或密码错误
        if (userDO == null) {
            throw new UserException("登录失败，请确认用户是否注册或者用户名和密码是否正确");
        }
        //到这里就是用户存在，能获取到token就是用户已经登录
        Long userId = userDO.getUserId();
        String token = redisTemplate.opsForValue().get(RedisKeys.USER_TOKEN_PREFIX + userId);
        //获取到了token，说明用户已经登录
        if (token != null) {
            log.warn("登录方法访问注册生成的token为：{}", token);
            throw new UserException("当前用户已经登录，请不要重复登录");
        }
        //没获取到token，说明用户已经很久没有登录了，注册时候创建的token失效了
        // 此时新创建token，然后将token缓存到redis中
        token = JwtUtils.createToken(userId, username);
        Integer userTokenExpireTime = randomUtils.createRandomTime();
        redisTemplate.opsForValue().set(RedisKeys.USER_TOKEN_PREFIX + userId, token, userTokenExpireTime, TimeUnit.MINUTES);

        log.warn("登录重新生成的token为：{}", token);
        //返回封装的结果
        return new UserDTO(userDO, "login:token:" + token);
    }


    /**
     * @author zzzi
     * @date 2024/3/27 10:50
     * 获取当前登录用户全部信息
     * 缓存中没有就从数据库中获取，重建缓存
     */
    @Override
    public UserVO getUserInfo(String user_id) {
        log.info("获取用户信息service，待查询的用户id为：{}", user_id);

        String userDOJson = redisTemplate.opsForValue().get(RedisKeys.USER_INFO_PREFIX + user_id);
        UserVO userVO = null;
        //缓存中获取到当前用户的信息
        if (userDOJson != null) {
            //打包需要的信息返回
            userVO = packageUserVO(userDOJson);
        } else {//缓存中没有
            try {
                //双重检查
                /**@author zzzi
                 * @date 2024/3/31 13:11
                 * 互斥锁加的时候，不能与原来的键冲突
                 * 并且加锁时锁的是当前线程
                 */
                long currentThreadId = Thread.currentThread().getId();
                Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.USER_INFO_PREFIX + user_id + "_mutex", currentThreadId + "");
                if (!absent) {
                    //不停地调用自己
                    Thread.sleep(50);
                    UserService userService = (UserService) AopContext.currentProxy();
                    userService.getUserInfo(user_id);
                }
                //再次尝试从缓存中获取
                userDOJson = redisTemplate.opsForValue().get(RedisKeys.USER_INFO_PREFIX + user_id);
                //缓存中获取到当前用户的信息
                if (userDOJson != null) {
                    //打包需要的信息返回
                    userVO = packageUserVO(userDOJson);
                } else {
                    userVO = rebuildUserInfoCache(user_id);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new UserInfoException("获取用户信息失败");
            } finally {
                /**@author zzzi
                 * @date 2024/3/31 15:13
                 * 这里需要判断删除互斥锁的是不是当前进程
                 */
                String currentThreadId = Thread.currentThread().getId() + "";
                String threadId = redisTemplate.opsForValue().get(RedisKeys.USER_INFO_PREFIX + user_id + "_mutex");
                //加锁的就是当前线程才解锁
                if (threadId.equals(currentThreadId)) {
                    redisTemplate.delete(RedisKeys.USER_INFO_PREFIX + user_id + "_mutex");
                }
            }
        }
        return userVO;
    }

    /**
     * @author zzzi
     * @date 2024/3/31 13:08
     * 重建用户信息缓存
     */
    public UserVO rebuildUserInfoCache(String user_id) {
        log.info("重建用户信息缓存service,user_id为：{}", user_id);
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getUserId, Long.valueOf(user_id));
        UserDO userDO = userMapper.selectOne(queryWrapper);
        String userDOJson = null;
        //数据库中也没有，此时缓存中需要保存默认值,并且需要抛出异常
        if (userDO == null) {
            redisTemplate.opsForValue().set(RedisKeys.USER_INFO_PREFIX + user_id, RedisDefaultValue.REDIS_DEFAULT_VALUE);
            //默认值5分钟过期
            redisTemplate.expire(RedisKeys.USER_INFO_PREFIX + user_id, 5, TimeUnit.MINUTES);
            userDOJson = RedisDefaultValue.REDIS_DEFAULT_VALUE;
        } else {
            //到这里就是查询到了真的数据
            Gson gson = new Gson();
            userDOJson = gson.toJson(userDO);
            //重建缓存,先删除默认值，其实这里不用删，因为String会被覆盖
            //redisTemplate.delete(RedisKeys.USER_INFO_PREFIX + user_id);
            redisTemplate.opsForValue().set(RedisKeys.USER_INFO_PREFIX + user_id, userDOJson);
            //更新用户token有效期
            updateTokenUtils.updateTokenExpireTimeUtils(user_id);
        }
        //最后打包
        return packageUserVO(userDOJson);
    }


    /**
     * @author zzzi
     * @date 2024/3/31 12:46
     * 根据从缓存中获取到的用户信息封装前端需要的信息
     */
    public UserVO packageUserVO(String userDOJson) {
        log.info("打包用户信息service");
        //防止缓存穿透的默认值
        if (RedisDefaultValue.REDIS_DEFAULT_VALUE.equals(userDOJson)) {
            return null;
        }
        //这里就是真正的缓存中的用户信息
        Gson gson = new Gson();
        UserDO userDO = gson.fromJson(userDOJson, UserDO.class);

        UserVO userVO = new UserVO();
        userVO.setId(userDO.getUserId());
        userVO.setName(userDO.getUsername());
        userVO.setFollow_count(userDO.getFollowCount());
        userVO.setFollower_count(userDO.getFollowerCount());
        userVO.setAvatar(userDO.getAvatar());
        userVO.setBackground_image(userDO.getBackgroundImage());
        userVO.setSignature(userDO.getSignature());
        userVO.setTotal_favorited(userDO.getTotalFavorited());
        userVO.setWork_count(userDO.getWorkCount());
        userVO.setFavorite_count(userDO.getFavoriteCount());
        return userVO;
    }


    /**
     * @author zzzi
     * @date 2024/3/26 22:54
     * 根据用户名和用户密码查询用户
     */
    public UserDO getUserDOByPasswordAndUserName(String username, String password) {
        log.info("根据用户名和密码获取用户实体service");
        //根据密码使用MD5加盐或者BCrypt自适应算法计算出来的加密值去数据库比对

        //，逐渐增加密码生成的迭代次数，使加密和解密时间变长
        //BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(16);
        //String pwdMD5 = encoder.encode(password);
        String pwdMD5 = MD5Utils.parseStrToMd5L32(password);
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        //用户名和密码都相等才算登陆成功，才能查询到用户信息
        queryWrapper.eq(UserDO::getUsername, username);
        queryWrapper.eq(UserDO::getPassword, pwdMD5);

        UserDO userDO = userMapper.selectOne(queryWrapper);

        //返回查询到的userDO
        return userDO;
    }
}
