package com.zzzi.common.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;


/**
 * @author zzzi
 * @date 2024/3/30 14:40
 * 在这里生成一个给定范围的随机值用来将缓存的过期时间随机打散
 * 防止缓存雪崩
 */
@Slf4j
@Component
public class RandomUtils {
    @Value("${random_start}")
    private int start;
    @Value("${random_end}")
    private int end;

    /**
     * @author zzzi
     * @date 2024/3/30 14:45
     * 返回一个随机的过期值，有最小边界
     * 使用ThreadLocalRandom是为了防止多线程下由于竞争导致的效率问题
     */
    public Integer createRandomTime() {
        if (start == end) {
            int time = ThreadLocalRandom.current().nextInt(start, end + 31);
            log.info("生成随机的缓存过期时间为：{}", time);
            return time;
        }
        int time = ThreadLocalRandom.current().nextInt(start, end + 1);
        log.info("生成随机的缓存过期时间为：{}", time);
        return time;
    }
}
