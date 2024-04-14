package com.zzzi.common.utils;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20190711.SmsClient;
import com.tencentcloudapi.sms.v20190711.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20190711.models.SendSmsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * @author zzzi
 * @date 2024/4/14 15:31
 * 腾讯云SMS发送短信工具类
 */
@Slf4j
@Component
public class SendMessageUtils {
    @Autowired
    private SMSUtils smsUtils;

    //发送短信
    public boolean sendMessage(String phoneNum, String validCode, String expireTime) {
        try {
            //实例化认证对象
            Credential cred = new Credential(smsUtils.getSecretId(), smsUtils.getSecretKey());

            // 实例化一个http选项
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setReqMethod("POST");
            //超时时间
            httpProfile.setConnTimeout(60);
            //指定接入地域域名，默认就近地域接入域名为 sms.tencentcloudapi.com
            httpProfile.setEndpoint(smsUtils.getEndpoint());

            /* 非必要步骤:
             * 实例化一个客户端配置对象，可以指定超时时间等配置 */
            ClientProfile clientProfile = new ClientProfile();
            //签名加密算法
            clientProfile.setSignMethod(smsUtils.getSignMethod());
            clientProfile.setHttpProfile(httpProfile);
            SmsClient client = new SmsClient(cred, smsUtils.getRegion(), clientProfile);

            //实例化请求对象
            SendSmsRequest req = new SendSmsRequest();

            //设置短信应用ID
            String sdkAppId = smsUtils.getSdkAppId();
            req.setSmsSdkAppid(sdkAppId);

            //设置短信签名
            String signName = smsUtils.getSignName();
            req.setSign(signName);

            //短信模版ID
            String templateId = smsUtils.getTemplateId();
            req.setTemplateID(templateId);

            //设置模版中的参数，这里是具体的短信验证码和短信验证码的到期时间
            req.setTemplateParamSet(new String[]{validCode, expireTime});

            //设置要发送的手机号
            req.setPhoneNumberSet(new String[]{phoneNum});

            //发送短信并得到发送的响应结果
            SendSmsResponse res = client.SendSms(req);
            //查看结果结构
            log.info("短信发送结果的结构为：{}", SendSmsResponse.toJsonString(res));

            //由于一次只发送一条短信，所以只用拿到SendStatus中的第一个状态即可
            String resCode = res.getSendStatusSet()[0].getCode();
            return "Ok".equals(resCode);

        } catch (TencentCloudSDKException e) {
            log.error("给：{}发送短信失败", phoneNum);
            e.printStackTrace();
        }
        //这一步只是为了不报错，实际不会执行
        return false;
    }

    //生成位随机验证码，可以六位可以四位
    public String geneValidCode() {
        Random random = new Random();
        return (random.nextInt(900001) + 100000) + "";
        //return (random.nextInt(9001) + 1000) + "";
    }
}
