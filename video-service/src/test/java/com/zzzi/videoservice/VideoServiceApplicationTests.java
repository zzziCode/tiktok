package com.zzzi.videoservice;

import com.zzzi.common.feign.UserClient;
import com.zzzi.common.result.UserVO;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.videoservice.entity.VideoDO;
import com.zzzi.videoservice.mapper.CommentMapper;
import com.zzzi.videoservice.mapper.VideoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

@SpringBootTest
class VideoServiceApplicationTests {
    @Autowired
    private VideoMapper videoMapper;

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

    @Test
    void testParseDate() {
        VideoDO videoDO = videoMapper.selectById(1775447822198992898L);
        Date createTime = videoDO.getCreateTime();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
        String create_date = sdf.format(createTime);
        System.out.println(create_date);
    }
}
