package com.zzzi.videoservice.config;


import com.gitee.Jmysy.binlog4j.core.BinlogEvent;
import com.gitee.Jmysy.binlog4j.core.IBinlogEventHandler;
import com.gitee.Jmysy.binlog4j.springboot.starter.annotation.BinlogSubscriber;
import com.zzzi.common.constant.RedisDefaultValue;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.videoservice.entity.VideoDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author zzzi
 * @date 2024/4/8 12:10
 * 在这里监听mysql中binlog的变化，从而完成缓存同步
 */
@Slf4j
@BinlogSubscriber(clientName = "master")
public class BinLogEventHandler implements IBinlogEventHandler<VideoDO> {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${user_works_max_size}")
    public Long USER_WORKS_MAX_SIZE;

    @Override
    public void onInsert(BinlogEvent<VideoDO> binlogEvent) {
        log.info("监听到视频表的插入");
        Long authorId = binlogEvent.getData().getAuthorId();
        Long videoId = binlogEvent.getData().getVideoId();

        //如果用户作品列表中有默认值，此时先删除默认值再添加
        List<String> userWorkList = redisTemplate.opsForList().range(RedisKeys.USER_WORKS_PREFIX + authorId, 0, -1);
        if (userWorkList.contains(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
            log.info("删除用户作品列表缓存的默认值");
            redisTemplate.delete(RedisKeys.USER_WORKS_PREFIX + authorId);
        }
        //先插入当前投稿的作品信息
        redisTemplate.opsForList().leftPush(RedisKeys.USER_WORKS_PREFIX + authorId, videoId + "");
        while (redisTemplate.opsForList().size(RedisKeys.USER_WORKS_PREFIX + authorId) > USER_WORKS_MAX_SIZE) {
            //从右边删除，代表删除最早投稿的视频
            redisTemplate.opsForList().rightPop(RedisKeys.USER_WORKS_PREFIX + authorId);
        }
    }

    @Override
    public void onUpdate(BinlogEvent<VideoDO> binlogEvent) {
        log.info("监听到视频表的更新");
        VideoDO data = binlogEvent.getData();

        System.out.println(data);
    }

    @Override
    public void onDelete(BinlogEvent<VideoDO> binlogEvent) {
        log.info("监听到视频表的删除");
        VideoDO data = binlogEvent.getData();

        System.out.println(data);

    }

    @Override
    public boolean isHandle(String s, String s1) {
        log.info("监听的数据库为：{}，表名为：{}", s, s1);
        //变化的表是tiktok中的video_{1..8}是才触发当前handler的执行
        return s.equals("tiktok") && Pattern.matches("^video_[1-8]$", s1);
    }
}
