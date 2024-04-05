package com.zzzi.userservice;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.common.utils.MD5Utils;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

//@SpringBootTest
class UserServiceApplicationTests {

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
}
