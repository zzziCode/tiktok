### RabbitMQ设计

> 在这里设计RabbitMQ需要接收哪些消息，交换机和队列的名称是什么

![image-20240407145837133](https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/img/202404071458593.png)

### Direct模式

这个模式涉及到交换机，一个交换机下有多个队列，每个队列都有自己的事情，例如点赞和取消点赞都在FAVORITE_EXCHANGE交换机下，但是消费的消息是相反的

尽量做到了单一职责，每个队列只负责单一的事情

### WorkQueue模式

这个模式下不涉及到交换机，只有四个工作模式的队列，每个队列下有两个消费者，目的是为了提高异步消息处理的速度

### RabbitMQKeys

RabbitMQ涉及到的常量如下：

```java
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
     WorkQueue模式
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
```

