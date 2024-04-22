package com.zzzi.common.utils;

import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
public class MinioUploadUtils {
    @Autowired
    private MinioUtils minioUtils;

    public String upload(File file, String suffix) throws InvalidPortException, InvalidEndpointException, IOException {
        // 初始化minio客户端
        MinioClient client = new MinioClient(minioUtils.getEndPoint(), minioUtils.getAccessKey(), minioUtils.getSecretKey());

        //获取文件输入流
        InputStream inputStream = new FileInputStream(file);
        try {
            // 生成唯一文件名，当前时间 +UUID+ 文件类型
            //指定文件保存的路径为存储桶下面的tiktok/文件夹下
            String choice = suffix.equals("_cover.jpg") ? "cover/" : "video/";
            String fileName = "tiktok/" + choice + LocalDateTime.now() + UUID.randomUUID() + suffix;

            //上传文件
            client.putObject(minioUtils.getBucketName(), fileName, inputStream, new PutObjectOptions(inputStream.available(), -1));

            //返回上传的URL路径
            String url = client.getObjectUrl("tiktok", fileName);	//文件访问路径
            return url;
        } catch (Exception e) {
            throw new RuntimeException("上传失败");
        } finally {
            inputStream.close();
        }
    }
}
