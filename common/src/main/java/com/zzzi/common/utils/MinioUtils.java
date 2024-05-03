package com.zzzi.common.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioUtils {
    private String endPoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

}
