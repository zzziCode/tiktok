package com.zzzi.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zzzi
 * @date 2024/4/13 14:12
 * 在这里配置Redisson客户端的地址，不采用起步依赖的方式整合springboot
 * 而是手动加入
 */
@Configuration
public class RedissonConfig {

    /**
     * @author zzzi
     * @date 2024/4/13 14:14
     * 如果Redis集群模式下需要使用MultiLock
     * 那么就在这里连接多个Redisson客户端即可
     * MultiLock眼中所有的Redis节点之间没有主从之分
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");

        //创建客户端
        return Redisson.create(config);
    }
}
