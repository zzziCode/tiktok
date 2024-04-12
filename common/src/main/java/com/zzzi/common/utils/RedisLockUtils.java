package com.zzzi.common.utils;

import cn.hutool.core.lang.UUID;
import com.zzzi.common.constant.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author zzzi
 * @date 2024/4/12 15:14
 * 在这里实现Redis的分布式锁
 */
@Component
@Slf4j
public class RedisLockUtils {

    @Resource
    private StringRedisTemplate redisTemplate;
    /**
     * @author zzzi
     * @date 2024/4/12 15:24
     * 每一个JVM对应一个独立的UUID
     * 因为他只会初始化一次，是一个常量
     * 使用这个的目的是为了解决集群中不同JVM内部可能出现线程id重复的问题
     */
    private static final String VALUE_PREFIX = UUID.randomUUID().toString(true) + "-";

    /**
     * @author zzzi
     * @date 2024/4/12 15:23
     * 加锁操作，由于key是固定的，所以这里加锁解锁都是可行的
     * 生成uuid的目的是为了防止线程id重复误删锁
     */
    public boolean lock(String key, long time, TimeUnit timeUnit) {
        log.info("给：{}加锁", key);
        //获取当前线程id
        long currentThreadId = Thread.currentThread().getId();

        //加锁时防止不同JVM中线程id一样加锁失败或者误删锁
        Boolean absent = redisTemplate.opsForValue().setIfAbsent(key, VALUE_PREFIX + currentThreadId, time, timeUnit);
        return Boolean.TRUE.equals(absent);
    }

    /**
     * @author zzzi
     * @date 2024/4/12 15:25
     * 解锁操作
     * 生成整个JVM内唯一的UUID是为了防止集群模式下不同的线程id重复导致误删锁
     */
    public void unlock(String key) {
        log.info("给：{}解锁", key);
        //获取当前线程id
        long currentThreadId = Thread.currentThread().getId();

        //获取要解锁的线程id
        String ThreadId = redisTemplate.opsForValue().get(key);
        //相等才释放，防止误删锁
        if ((VALUE_PREFIX + currentThreadId).equals(ThreadId))
            redisTemplate.delete(key);
    }
}
