package com.zzzi.common.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author zzzi
 * @date 2024/4/14 15:27
 * 腾讯云短信服务配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "tencent.sms")
public class SMSUtils {

    private String secretId;
    private String secretKey;
    private String endpoint;
    private String region;
    private String sdkAppId;
    private String signName;
    private String templateId;
    private String signMethod;

}
