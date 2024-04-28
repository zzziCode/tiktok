package com.zzzi.common.utils;


import com.zzzi.common.constant.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author zzzi
 * @date 2024/4/2 16:21
 * 在这里更新用户token的过期时间
 */
@Slf4j
@Component
public class UpdateTokenUtils {
    @Autowired
    private RandomUtils randomUtils;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void updateTokenExpireTimeUtils(String userId) {
        Integer userTokenExpireTime = randomUtils.createRandomTime();
        redisTemplate.expire(RedisKeys.USER_TOKEN_PREFIX + userId, userTokenExpireTime, TimeUnit.MINUTES);
    }
}
