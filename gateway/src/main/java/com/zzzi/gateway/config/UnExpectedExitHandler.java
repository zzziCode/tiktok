package com.zzzi.gateway.config;

import com.zzzi.common.constant.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @author zzzi
 * @date 2024/4/4 17:27
 * 程序意外退出执行这个方法
 */
@Component
@Slf4j
public class UnExpectedExitHandler implements DisposableBean {
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * @author zzzi
     * @date 2024/4/4 17:22
     * 系统崩溃时删除所有用户token，防止token没过期下次登录不上
     */
    @Override
    public void destroy() throws Exception {
        boolean state = false;
        Set<String> keys = redisTemplate.keys("*");
        for (String key : keys) {
            //只删除用户token
            if (key.startsWith(RedisKeys.USER_TOKEN_PREFIX))
                redisTemplate.delete(key);
            state = true;
        }
        if (state) {
            log.info("清除缓存成功！");
        } else {
            log.info("无缓存数据可清除！");
        }
    }
}
