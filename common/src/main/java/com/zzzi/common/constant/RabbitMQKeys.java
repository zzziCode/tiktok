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
    public static final String POST_VIDEO_EXCHANGE = "tiktok.post_video";
    public static final String FOLLOW_EXCHANGE = "tiktok.follow";
    public static final String COMMENT_EXCHANGE = "tiktok.comment";

    /**
     * MQ中关于点赞的queue
     * 点赞和取消点赞需要分开
     * 并且点赞针对用户和视频的key也需要分开
     */
    public static final String FAVORITE_USER = "work.favorite_user";
    public static final String UN_FAVORITE_USER = "work.un_favorite_user";
    public static final String FAVORITE_VIDEO = "work.favorite_video";
    public static final String UN_FAVORITE_VIDEO = "work.un_favorite_video";
    /**
     * MQ中关于评论的key
     */
    public static final String COMMENT_KEY = "comment";
    public static final String UN_COMMENT_KEY = "un_comment";
    /**
     * MQ中关于关注的key
     * 关注和取消关注需要分开
     */
    public static final String FOLLOW_KEY = "follow";
    public static final String UN_FOLLOW_KEY = "un_follow";

    /**
     * MQ中关于投稿的key
     */
    public static final String VIDEO_POST = "video_post";
}
