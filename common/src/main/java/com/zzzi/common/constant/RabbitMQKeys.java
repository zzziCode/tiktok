package com.zzzi.common.constant;

/**
 * @author zzzi
 * @date 2024/3/25 18:46
 * RabbitMQ的相关设计
 */
public class RabbitMQKeys {

    /**
     * MQ中交换机名称
     */
    public static final String EXCHANGE_NAME = "tiktok.direct";

    /**
     * MQ中关于点赞的key
     */
    public static final String FAVORITE_KEY = "favorite";
    /**
     * MQ中关于评论的key
     */
    public static final String COMMENT_KEY = "comment";
    /**
     * MQ中关于关注的key
     * 关注和取消关注需要分开
     */
    public static final String FOLLOW_KEY = "follow";
    public static final String UN_FOLLOW_KEY = "follow";

    /**
     * MQ中关于关注的key
     */
    public static final String VIDEO_POST = "video_post";


}
