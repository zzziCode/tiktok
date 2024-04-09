package com.zzzi.videoservice.config;

import com.zzzi.common.constant.RabbitMQKeys;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
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

    /**
     * @author zzzi
     * @date 2024/4/9 16:01
     * 消费者消费消息失败时会进行重试
     * 重试次数达到设置的上限时会将失败的消息投放到这个队列中
     */

    //定义错误消息的交换机
    @Bean
    public DirectExchange errorMessageExchange() {
        return new DirectExchange(RabbitMQKeys.ERROR_EXCHANGE);
    }

    //定义错误消息的队列
    @Bean
    public Queue errorQueue() {
        return new Queue("error.queue", true);
    }

    //定义二者之间的绑定关系
    @Bean
    public Binding errorBinding(Queue errorQueue, DirectExchange errorMessageExchange) {
        return BindingBuilder.bind(errorQueue).to(errorMessageExchange).with(RabbitMQKeys.ERROR);
    }

    @Bean
    public MessageRecoverer republishMessageRecoverer(RabbitTemplate rabbitTemplate) {
        //重试次数耗尽时，会将失败的消息发送给指定的队列
        return new RepublishMessageRecoverer(rabbitTemplate, RabbitMQKeys.ERROR_EXCHANGE, RabbitMQKeys.ERROR);
    }

}
