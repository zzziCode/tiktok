package com.zzzi.userservice;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.common.utils.MD5Utils;
import com.zzzi.common.utils.SendMessageUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

//@SpringBootTest
class UserServiceApplicationTests {
    @Autowired
    private SendMessageUtils sendMessageUtils;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void contextLoads() {
    }

    /**
     * @author zzzi
     * @date 2024/3/27 13:57
     * 测试用户名和用户id一致时，前后生成的token是否一致
     */
    @Test
    void testToken() {
        Long userId = 1500000000000000000L;
        String userName = "123";
        String token1 = JwtUtils.createToken(userId, userName);
        String token2 = JwtUtils.createToken(userId, userName);
        System.out.println(token1);
        System.out.println(token2);
        System.out.println(token1.equals(token2));
    }

    @Test
    void testParseToken() {
        String token = "eyJhbGciOiJIUzUxMiIsInppcCI6IkdaSVAifQ.H4sIAAAAAAAAAKtWKi5NUrJSconUDQ12DVLSUUqtKFCyMjQ3NLSwsDAytdBRKi1OLfJMAYqZGqADiKRfYm4q0AhDI2OlWgAB1h9IUAAAAA.1S3xfjQNKSeR6ytrMN3tJBh9CkH4qOINVWMCAWXJZGJF5SzU6nMRyejpfLQbQ0iTQXrbjh_uN6UJKWRiMY28Ww";
        Long userIdByToken = JwtUtils.getUserIdByToken(token);
        System.out.println(userIdByToken);
    }

    @Test
    void testBCryptPasswordEncoder() {
        // Create an encoder with strength 16
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(16);
        String result = encoder.encode("123456");
        System.out.println(result);
    }

    @Test
    void testMD5Salt() {
        String passMD5 = MD5Utils.parseStrToMd5L32("123456");
        System.out.println(passMD5);
    }


    /**
     * @author zzzi
     * @date 2024/4/14 16:06
     * 用户手机号登录时的步骤：
     * 1. 先利用正则表达式判断手机号是否合法
     * 2. 根据手机号查询是否有该用户的信息：先查缓存在查数据库
     * 3. 有该用户就生成验证码并发送，否则返回请先注册
     * 4. 将验证码和用户手机号的映射关系使用String存储到Redis中
     * 5. 用户输入验证码点击登录，携带手机号和验证码发送请求
     * 6. 收到请求之后进行判断
     *    6.1 验证码过期返回 “验证码已过期”
     *    6.2 验证码匹配失败返回 “验证码验证失败”
     *    6.3 匹配成功放行跳转登录
     */
    @Test
    void testSendMessage() {
        //获取6位验证码
        String validCode = sendMessageUtils.geneValidCode();

        String phoneNum = "17729233402";
        Long expireTime = 2L;
        //发送短信，默认两分钟过期
        boolean flag = sendMessageUtils.sendMessage(phoneNum, validCode, expireTime.toString());
        if (!flag)
            System.out.println("验证码发送失败");
        //后期可以将这个短信保存到redis中
        redisTemplate.opsForValue().set(RedisKeys.USER_VALID_CODE_PREFIX + phoneNum, validCode, expireTime, TimeUnit.MINUTES);

        //用户登录携带手机号和验证码就可以完成登录
        //登录时判断从redis中取出对应的验证码，然后判断是否有
    }
}
