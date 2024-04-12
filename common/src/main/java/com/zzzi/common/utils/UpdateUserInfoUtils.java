package com.zzzi.common.utils;

import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author zzzi
 * @date 2024/3/29 21:40
 * 在这里统一更新用户的缓存
 */
@Component
@Slf4j
public class UpdateUserInfoUtils {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UpdateUserInfoUtils updateUserInfoUtils;

    /**
     * @author zzzi
     * @date 2024/3/31 15:44
     * 先更新数据库再更新缓存
     */
    public void updateUserInfoCache(Long authorId, String userDOJson) {
        //todo 实现AP模式，牺牲一致性，拿到的可能是旧数据，但是保证业务可用
        String mutex = MD5Utils.parseStrToMd5L32(userDOJson);
        try {
            //拿到互斥锁
            /**@author zzzi
             * @date 2024/3/31 15:37
             * 当前线程加上互斥锁，防止大量重建请求同时到达数据库造成数据库压力过大：解决缓存击穿
             * 加上锁之后只有一个线程缓存重建
             * 同时设置用户信息不过期，进一步防止缓存击穿
             */
            long currentThreadId = Thread.currentThread().getId();
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.MUTEX_LOCK_PREFIX + mutex, currentThreadId + "", 1, TimeUnit.MINUTES);

            //没拿到互斥锁说明当前用户正在被修改，应该重试
            if (!absent) {
                Thread.sleep(50);
                //调用自己重试，防止事务失效，这里要注入自己
                updateUserInfoUtils.updateUserInfoCache(authorId, userDOJson);
            }

            /**@author zzzi
             * @date 2024/3/29 13:24
             * 更新用户缓存，直接覆盖旧值
             */
            redisTemplate.opsForValue().set(RedisKeys.USER_INFO_PREFIX + authorId, userDOJson);

        } catch (Exception e) {
            log.error(e.getMessage());
            //手动回滚
            throw new UserException("更新用户信息失败");
        } finally {//最后释放互斥锁
            /**@author zzzi
             * @date 2024/3/31 15:37
             * 需要是加锁的线程才能解锁
             */
            String currentThreadId = Thread.currentThread().getId() + "";
            String threadId = redisTemplate.opsForValue().get(RedisKeys.MUTEX_LOCK_PREFIX + mutex);
            //加锁的就是当前线程才解锁
            if (currentThreadId.equals(threadId)) {
                redisTemplate.delete(RedisKeys.MUTEX_LOCK_PREFIX + mutex);
            }
        }
    }
}
