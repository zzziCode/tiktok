package com.zzzi.videoservice;

import com.alibaba.cloud.nacos.ribbon.NacosRule;
import com.netflix.loadbalancer.IRule;
import com.zzzi.common.config.DefaultFeignConfiguration;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.feign.UserClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PreDestroy;
import java.util.Set;

@SpringBootApplication(scanBasePackages = {"com.zzzi.*"})
@EnableTransactionManagement//开启事务管理
@EnableFeignClients(clients = UserClient.class, defaultConfiguration = DefaultFeignConfiguration.class)
public class VideoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoServiceApplication.class, args);
    }

    //负载均衡规则
    @Bean
    public IRule rule() {
        return new NacosRule();
    }
}
