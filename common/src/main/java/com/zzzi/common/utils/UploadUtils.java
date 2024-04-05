package com.zzzi.common.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
public class UploadUtils {

    @Autowired
    private COSUtils cosUtils;

    /**
     * @author zzzi
     * @date 2024/3/23 21:56
     * 根据传递而来的文件以及对应的后缀将文件上传到对应的文件夹中
     * 后缀为.jpg就是上传cover，否则就是上传视频
     */
    public String upload(File file, String suffix) {
        // 初始化cos客户端
        COSCredentials cred = new BasicCOSCredentials(cosUtils.getSecretId(), cosUtils.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cosUtils.getRegion()));
        COSClient cosClient = new COSClient(cred, clientConfig);
        try {
            // 生成唯一文件名，当前时间 +UUID+ 文件类型
            //指定文件保存的路径为存储桶下面的tiktok/文件夹下
            String choice = suffix.equals("_cover.jpg") ? "cover/" : "video/";
            String fileName = "tiktok/" + choice + LocalDateTime.now() + UUID.randomUUID() + suffix;

            // 上传文件到cos,上传的核心步骤
            PutObjectRequest putObjectRequest = new PutObjectRequest(cosUtils.getBucketName(), fileName, file);
            cosClient.putObject(putObjectRequest);
            // 返回文件在cos上的访问url,直接拼接起来
            //https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok/video/2024-03-24T10:28:33.668779d646a-8c32-4c53-b6df-23958a72cd31_video.mp4
            String url = "https://" + cosUtils.getBucketName() + ".cos." + cosUtils.getRegion() + ".myqcloud.com/" + fileName;

            //返回上传的URL路径
            return url;
        } catch (Exception e) {
            throw new RuntimeException("上传失败");
        } finally {
            // 关闭cos客户端
            cosClient.shutdown();
        }
    }
}
