package com.zzzi.videoservice;

import com.zzzi.common.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

//@SpringBootTest
class VideoServiceApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testParseUserId() {
        String token = "eyJhbGciOiJIUzUxMiIsInppcCI6IkdaSVAifQ.H4sIAAAAAAAAAKtWKi5NUrJSconUDQ12DVLSUUqtKFCyMjQ3NDQzMLc0M9BRKi1OLfJMAYmZG1mYmxkYW1haGJuYmpgbGUEk_RJzU4FGGBoaOhQW6iXn5yrVAgBhI68YVwAAAA.tYVj46twIZzN1lJbbeelUqSt50_1zcS1Oujp9NL3WrUKYD7MSgYQE-CqdJiLnM5StVrBm-5dXfLotmyusGUjNg";
        Long id = JwtUtils.getUserIdByToken(token);
        System.out.println(id);
    }

    @Test
    void testSubToken() {
        String token = "login:token:" + "eyJhbGciOiJIUzUxMiIsInppcCI6IkdaSVAifQ.H4sIAAAAAAAAAKtWKi5NUrJSconUDQ12DVLSUUqtKFCyMjQ3NDQzMLc0M9BRKi1OLfJMAYmZG1mYmxkYW1haGJuYmpgbGUEk_RJzU4FGGBoaOhQW6iXn5yrVAgBhI68YVwAAAA.tYVj46twIZzN1lJbbeelUqSt50_1zcS1Oujp9NL3WrUKYD7MSgYQE-CqdJiLnM5StVrBm-5dXfLotmyusGUjNg";
        token = token.substring(12);
        System.out.println(token);
    }
}
