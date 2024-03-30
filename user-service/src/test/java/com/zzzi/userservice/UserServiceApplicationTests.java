package com.zzzi.userservice;

import com.zzzi.common.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
        System.out.println(token1.equals(token2));
    }

    @Test
    void testParseToken(){
        String token="eyJhbGciOiJIUzUxMiIsInppcCI6IkdaSVAifQ.H4sIAAAAAAAAAKtWKi5NUrJSconUDQ12DVLSUUqtKFCyMjQ3NLSwsDAytdBRKi1OLfJMAYqZGqADiKRfYm4q0AhDI2OlWgAB1h9IUAAAAA.1S3xfjQNKSeR6ytrMN3tJBh9CkH4qOINVWMCAWXJZGJF5SzU6nMRyejpfLQbQ0iTQXrbjh_uN6UJKWRiMY28Ww";
        Long userIdByToken = JwtUtils.getUserIdByToken(token);
        System.out.println(userIdByToken);
    }
}
