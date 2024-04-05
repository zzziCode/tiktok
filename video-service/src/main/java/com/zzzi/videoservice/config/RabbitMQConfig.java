package com.zzzi.videoservice.config;

import com.zzzi.common.constant.RabbitMQKeys;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zzzi
 * @date 2024/3/27 16:24
 * 这可以使得RabbitMQ使用json序列化，而不是使用java中的jdk序列化
 */
@Configuration
public class RabbitMQConfig {
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * @author zzzi
     * @date 2024/4/4 19:09
     * 定义两个Work模式的队列，监听点赞消息，用来更新视频信息
     */
    @Bean
    public Queue createFavoriteVideoQueue() {
        return new Queue(RabbitMQKeys.FAVORITE_VIDEO);
    }

    @Bean
    public Queue createUnFavoriteVideoQueue() {
        return new Queue(RabbitMQKeys.UN_FAVORITE_VIDEO);
    }
}
