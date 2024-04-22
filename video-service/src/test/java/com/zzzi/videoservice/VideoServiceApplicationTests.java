package com.zzzi.videoservice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.common.utils.MinioUploadUtils;
import com.zzzi.videoservice.entity.VideoDO;
import com.zzzi.videoservice.mapper.VideoMapper;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.xml.transform.Source;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;

@SpringBootTest
class VideoServiceApplicationTests {
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private MinioUploadUtils minioUploadUtils;

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

    @Test
    void testTime() {
        System.out.println("1:" + System.currentTimeMillis());
        long millis = System.currentTimeMillis();

        LambdaQueryWrapper<VideoDO> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.gt(VideoDO::getUpdateTime, millis);
        List<VideoDO> videoDOList = videoMapper.selectList(queryWrapper);

        for (VideoDO videoDO : videoDOList) {
            System.out.println("每个视频的时间：" + videoDO.getUpdateTime().getTime());
        }
    }

    @Test
    void testMinio() throws IOException, InvalidPortException, InvalidEndpointException {
        File file = new File("C:\\Users\\zzzi\\Desktop\\3daec69277cdeac317257f588ee.mp4");
        String upload = minioUploadUtils.upload(file, ".,mp4");
        System.out.println(upload);
    }
}
