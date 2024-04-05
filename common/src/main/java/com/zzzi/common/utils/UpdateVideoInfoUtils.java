package com.zzzi.common.utils;

import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.VideoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author zzzi
 * @date 2024/3/29 21:40
 * 在这里统一更新视频信息的缓存
 */
@Component
@Slf4j
public class UpdateVideoInfoUtils {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UpdateVideoInfoUtils updateUserInfoUtils;

    /**
     * @author zzzi
     * @date 2024/3/31 15:44
     * 先更新数据库再更新缓存
     */
    public void updateVideoInfoCache(Long videoId, String videoDOJson) {
        String mutex = MD5Utils.parseStrToMd5L32(videoDOJson);
        try {
            //拿到互斥锁
            /**@author zzzi
             * @date 2024/3/31 15:37
             * 当前线程加上互斥锁
             */
            long currentThreadId = Thread.currentThread().getId();
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.MUTEX_LOCK_PREFIX + mutex, currentThreadId + "");

            //没拿到互斥锁说明当前视频正在被修改，应该重试
            if (!absent) {
                Thread.sleep(50);
                //调用自己重试，防止事务失效，这里要注入自己
                updateUserInfoUtils.updateVideoInfoCache(videoId, videoDOJson);
            }

            /**@author zzzi
             * @date 2024/3/29 13:24
             * 更新视频缓存，直接覆盖旧值
             */
            redisTemplate.opsForValue().set(RedisKeys.VIDEO_INFO_PREFIX + videoId, videoDOJson);

        } catch (Exception e) {
            log.error(e.getMessage());
            //手动回滚
            throw new VideoException("更新视频信息失败");
        } finally {//最后释放互斥锁
            /**@author zzzi
             * @date 2024/3/31 15:37
             * 需要是加锁的线程才能解锁
             */
            String currentThreadId = Thread.currentThread().getId() + "";
            String threadId = redisTemplate.opsForValue().get(RedisKeys.MUTEX_LOCK_PREFIX + mutex);
            //加锁的就是当前线程才解锁
            if (threadId.equals(currentThreadId)) {
                redisTemplate.delete(RedisKeys.MUTEX_LOCK_PREFIX + mutex);
            }
        }
    }
}
