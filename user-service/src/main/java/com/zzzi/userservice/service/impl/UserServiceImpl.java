package com.zzzi.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.sun.corba.se.impl.oa.toa.TOA;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.common.utils.MD5Utils;
import com.zzzi.userservice.dto.UserDTO;
import com.zzzi.userservice.entity.UserDO;
import com.zzzi.common.exception.UserException;
import com.zzzi.userservice.mapper.UserMapper;
import com.zzzi.common.result.UserVO;
import com.zzzi.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
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
    /**
     * @author zzzi
     * @date 2024/3/27 10:27
     * 注入StringRedisTemplate而不是RedisTemplate是因为Redis中存储的就是String类型的数据
     */
    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * @author zzzi
     * @date 2024/3/26 21:30
     * 用户注册之后返回一个用户的token
     * 由于需要操作数据库，所以需要加上事务
     */
    @Override
    @Transactional
    public UserDTO register(String username, String password) {

        //正则表达式验证邮箱合法性
        String pattern = "^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$";
        if (!Pattern.matches(pattern, username)) {
            throw new UserException("邮箱格式不正确！");
        }
        // 数据库中存的是MD5加密以后的值
        String pwdMD5 = MD5Utils.parseStrToMd5L32(password);


        //尝试往数据库中插入数据，插入失败说明用户名被占用
        try {
            UserDO userDO = new UserDO();
            userDO.setUsername(username);
            userDO.setEmail(username);
            userDO.setPassword(pwdMD5);
            userMapper.insert(userDO);

            Long userId = userDO.getUserId();
            String token = JwtUtils.createToken(userId, username);

            // 将当前注册用户的token进行缓存
            // 在30分钟内登录就直接使用当前缓存，当前缓存过期才重新生成token
            redisTemplate.opsForValue().set(RedisKeys.USER_TOKEN_PREFIX + userId, token, 30, TimeUnit.MINUTES);

            //返回封装好的对象
            return new UserDTO(userDO, token);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new UserException("用户名被占用，请重新输入用户名");
        }
    }

    /**
     * @author zzzi
     * @date 2024/3/26 21:28
     * 判断用户是否存在，存在将用户信息缓存到redis中
     */
    @Override
    public UserDTO login(String username, String password) {
        UserDO userDO = getUserDOByPasswordAndUserName(username, password);
        //没有该用户，用户名或密码错误
        if (userDO == null) {
            throw new UserException("登录失败，请确认是否注册或者用户名和密码是否正确");
        }
        //到这里就是用户存在，此时先尝试获取现有token，没有再生成
        Long userId = userDO.getUserId();
        String token = redisTemplate.opsForValue().get(RedisKeys.USER_TOKEN_PREFIX + userId);
        //没有才创建新token
        if (token == null || "".equals(token)) {
            token = JwtUtils.createToken(userId, username);
        }

        //将用户转换成json数据，然后存储到redis中
        Gson gson = new Gson();
        String userDOJson = gson.toJson(userDO);
        /**@author zzzi
         * @date 2024/3/26 23:09
         * 将用户登录信息保存到String中，并设置过期值30分钟
         * 后续一旦操作这个用户信息，过期时间自动更新
         * 后缀使用user_id
         */

        //保存用户登录信息之前，要判断用户是不是已经登录
        Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.USER_INFO_PREFIX + userId, userDOJson, 30, TimeUnit.MINUTES);
        //当前用户已经登录
        if (!absent) {
            throw new UserException("当前用户已经登录，请不要重复登录");
        }
        //登陆之后更新用户的token有效期
        redisTemplate.expire(RedisKeys.USER_TOKEN_PREFIX + userId, 30, TimeUnit.MINUTES);

        //返回封装的结果
        return new UserDTO(userDO, token);
    }


    /**
     * @author zzzi
     * @date 2024/3/27 10:50
     * 获取当前登录用户全部信息
     * 用户信息变动，此时需要修改缓存，而不是删除缓存
     */
    @Override
    public UserVO getUserInfo(String user_id) {
        log.info("待查询的用户id为：{}", user_id);

        String userDOJson = redisTemplate.opsForValue().get(RedisKeys.USER_INFO_PREFIX + user_id);

        //缓存中没有获取到当前用户的信息
        if (userDOJson == null || "".equals(userDOJson)) {
            throw new UserException("用户未登录，请先去登录");
        }

        //在这里就是获取到了用户登录信息，此时将其转换成java对象
        Gson gson = new Gson();
        UserDO userDO = gson.fromJson(userDOJson, UserDO.class);

        //用户信息获取成功，代表当前用户已登录并且有操作，此时更新token和用户信息的有效期
        redisTemplate.expire(RedisKeys.USER_TOKEN_PREFIX + user_id, 30, TimeUnit.MINUTES);
        redisTemplate.expire(RedisKeys.USER_INFO_PREFIX + user_id, 30, TimeUnit.MINUTES);

        //将查询到的userDO转换成前端需要的userVO
        return packageUserVO(userDO);
    }


    /**
     * @author zzzi
     * @date 2024/3/26 22:54
     * 根据用户名和用户密码查询用户
     */
    public UserDO getUserDOByPasswordAndUserName(String username, String password) {
        //根据密码获取加密后的MD5值去数据库比对
        String pwdMD5 = MD5Utils.parseStrToMd5L32(password);
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        //用户名和密码都相等才算登陆成功，才能查询到用户信息
        queryWrapper.eq(UserDO::getUsername, username);
        queryWrapper.eq(UserDO::getPassword, pwdMD5);

        UserDO userDO = userMapper.selectOne(queryWrapper);

        //返回查询到的userDO
        return userDO;
    }

    /**
     * @author zzzi
     * @date 2024/3/27 11:20
     * 封装前端需要的数据
     */
    public UserVO packageUserVO(UserDO userDO) {
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
     * @date 2024/3/27 11:15
     * 根据当前用户的id判断与userDO的关注关系
     */
    public UserVO packageUserVO(UserDO userDO, Long currentUserID) {
        return null;

    }

    /**
     * @author zzzi
     * @date 2024/3/27 15:22
     * 投稿时更新用户作品数
     */
    public void updateUserWorkCount(Long userId) {

    }


}
