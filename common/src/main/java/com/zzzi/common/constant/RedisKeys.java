package com.zzzi.common.constant;

/**
 * @author zzzi
 * @date 2024/3/25 13:03
 * 在这里设计所有的Redis中所有缓存的名称或者前缀
 */
public class RedisKeys {
    //存储用户登录信息的Hash的key
    public static final String USER_LOGIN = "user_login";

    //按照视频发布时间降序存储视频的Zset的key
    public static final String VIDEO_FEED = "video_feed";

    //缓存用户全部信息的Set的key的前缀
    public static final String USER_INFO_PREFIX = "user_info:";


    /**@author zzzi
     * @date 2024/3/25 16:23
     * 下面是可选项
     */
    //缓存用户所有作品的Set的key的前缀
    public static final String USER_WORKS_PREFIX = "user_works:";

    //缓存用户所有点赞作品的Set的key的前缀
    public static final String USER_FAVORITES_PREFIX = "user_favorites:";

    //缓存用户所有关注的Set的key的前缀
    public static final String USER_FOLLOWS_PREFIX = "user_follows:";

    //缓存用户所有粉丝的Set的key的前缀
    public static final String USER_FOLLOWERS_PREFIX = "user_followers:";

    //缓存视频所有评论的List的key的前缀
    public static final String VIDEO_COMMENTS_PREFIX = "video_comments:";
}
