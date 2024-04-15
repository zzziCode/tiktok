package com.zzzi.userservice.controller;


import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.result.UserInfoVO;
import com.zzzi.common.result.ValidCodeVO;
import com.zzzi.common.utils.SendMessageUtils;
import com.zzzi.userservice.dto.UserDTO;
import com.zzzi.common.result.UserRegisterLoginVO;
import com.zzzi.common.result.UserVO;
import com.zzzi.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


@RestController
@RequestMapping("/douyin/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * @author zzzi
     * @date 2024/3/27 10:38
     * 用户注册，会根据用户id和用户姓名创建一个token返回
     */
    @PostMapping("/register")
    public UserRegisterLoginVO register(String username, String password) {
        log.info("注册时的用户名为：{}，用户密码为：{}", username, password);
        UserDTO userDTO = userService.register(username, password);

        //封装需要的数据返回
        return UserRegisterLoginVO.success(userDTO.getUserDO().getUserId(), userDTO.getToken());
    }

    /**
     * @author zzzi
     * @date 2024/3/27 10:39
     * 用户登录，也会根据用户id和用户姓名创建一个token返回
     * 由于签名算法和签名秘钥一致，所以注册登录的token一致
     */
    @PostMapping("/login")
    public UserRegisterLoginVO login(@RequestParam(required = false) String username,
                                     @RequestParam(required = false) String password,
                                     @RequestParam(required = false) String phoneNum,
                                     @RequestParam(required = false) String validCode) {
        log.info("登录时的用户名为：{}，用户密码为：{}", username, password);
        log.info("登录时的手机号为：{}，验证码为：{}", phoneNum, validCode);
        UserDTO userDTO = null;
        //用用户名和密码登录
        if (username != null && !"".equals(username) && password != null && !"".equals(password)) {
            userDTO = userService.loginWithPassWord(username, password);
        }
        //用手机号和验证码登录
        else {
            userDTO = userService.loginWithValidCode(phoneNum, validCode);
        }
        //封装需要的数据返回
        return UserRegisterLoginVO.success(userDTO.getUserDO().getUserId(), userDTO.getToken());
    }

    /**
     * @author zzzi
     * @date 2024/3/27 10:39
     * 获取当前登录用户全部信息
     * 这个也可以用来做远程调用
     */
    @GetMapping
    public UserInfoVO userInfo(String user_id, @RequestParam(required = false) String token) {
        log.info("获取用户信息的用户id为：{}，用户token为：{}", user_id, token);
        if (token != null && token.startsWith("login:token:")) {
            token = token.substring(12);
        }
        log.info("截取之后的用户token为：{}", token);
        UserVO user = userService.getUserInfo(user_id);
        if (user == null)
            return UserInfoVO.fail("当前用户不存在，请先注册");
        //将后端封装好的userVO返回给前端
        return UserInfoVO.success(user);
    }

    //获取验证码的接口
    @GetMapping
    public ValidCodeVO getValidCode(String phoneNum) {
        String validCode = userService.getValidCode(phoneNum);
        if (validCode != null && !"".equals(validCode)) {
            return ValidCodeVO.success(validCode);
        }
        return ValidCodeVO.fail("生成验证码失败");
    }


}

