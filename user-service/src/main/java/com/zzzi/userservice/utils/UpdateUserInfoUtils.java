package com.zzzi.userservice.utils;

import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.UserException;
import com.zzzi.common.utils.MD5Utils;
import com.zzzi.common.utils.RandomUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

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

    /**@author zzzi
     * @date 2024/3/31 15:44
     * 先更新数据库再更新缓存
     */
    public void updateUserInfoCache(Long authorId, String userDOJson) {
        String mutex = MD5Utils.parseStrToMd5L32(userDOJson);
        try {
            //拿到互斥锁
            /**@author zzzi
             * @date 2024/3/31 15:37
             * 当前线程加上互斥锁
             */
            long currentThreadId = Thread.currentThread().getId();
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.MUTEX_LOCK_PREFIX + mutex, currentThreadId + "");

            //没拿到互斥锁说明当前用户正在被修改，应该重试
            if (!absent) {
                Thread.sleep(50);
                //调用自己重试
                updateUserInfoCache(authorId, userDOJson);
            }

            /**@author zzzi
             * @date 2024/3/29 13:24
             * 更新用户缓存，直接覆盖旧值
             */
            //加入，并设置三十分钟有效期
            redisTemplate.opsForValue().set(RedisKeys.USER_INFO_PREFIX + authorId, userDOJson);

        } catch (Exception e) {
            log.error(e.getMessage());
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new UserException("更新用户信息失败");
        } finally {//最后释放互斥锁
            /**@author zzzi
             * @date 2024/3/31 15:37
             * 需要是加锁的线程才能解锁
             */
            String currentThreadId = Thread.currentThread().getId() + "";
            String threadId = redisTemplate.opsForValue().get(RedisKeys.MUTEX_LOCK_PREFIX + mutex);
            //加锁的就是当前线程才解锁
            if (threadId.equals(currentThreadId)){
                redisTemplate.delete(RedisKeys.MUTEX_LOCK_PREFIX + mutex);
            }
        }
    }
}
