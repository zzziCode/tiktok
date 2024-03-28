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
        Long userId = 1772636762530189313L;
        String userName = "1111@qq.com";
        String token1 = JwtUtils.createToken(userId, userName);
        String token2 = JwtUtils.createToken(userId, userName);


        System.out.println(token1.equals(token2));
    }
}
