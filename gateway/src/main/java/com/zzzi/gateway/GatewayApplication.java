package com.zzzi.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}

/**
 * @author zzzi
 * @date 2024/3/30 13:27
 * todo：1. 分布式事务Seata + 多数据源管理（tiktok库读写分离，salt库读写不分离），用哪个数据源注入哪个数据源
 * 2. 防止缓存穿透，存储默认值，不使用布隆过滤器
 * todo: 3. 防止缓存雪崩：过期值打散+Sentinel限流
 * todo：4. 推拉模式的视频推流
 * 5. 用户作品缓存只缓存一部分新的，也就是leftPush之后超过长度需要rightPop
 * 6. 登录校验（有token说明已登录）
 * todo：7. 用户签到
 * todo：8. 改进RabbitMQ，实现异步通信的可靠性
 * todo：9. tiktok库一主多从复制+salt库分开，相当于动态数据源动态切换
 * todo：10. 本地视频获取路径
 * 11. 获取用户的关注列表
 * 12. 测试完善关注和取消关注
 * 13. 完善用户是否登录
 * 14. 测试try-catch是否吃掉回滚，也就是事务为什么失效，如何解决
 * 15. 注册之后直接调用接口获取用户基本信息
 * todo：16. 使用canal实现缓存和数据库中的数据一致性
 * 17. 先更新数据库再操作缓存，因为缓存速度快，尽可能减小数据不一致问题
 * 18. 分布式锁解锁问题，判断是不是当前线程来解锁（看原项目怎么写的）
 * 19. setIfAbsent的问题，用户信息应该是不过期的
 * 20. Redis默认值过期值为5分钟
 * 21. 排除所有空指针
 * 22. 注册成功就应该保存token
 * 23. is_follow代表我是否关注了当前用户，详见UserVO实体类中的解释
 * 24. 粉丝列表、好友列表，并进行测试，主要测试is_follow
 * 25. 上传功能改进，测试现有的所有功能
 * 26. 先将视频id缓存和视频缓存分离开
 * 27. 点赞/取消点赞，点赞列表
 * 28. 排查所有token的用途
 * 29. 所有QueryWrapper传递的判断条件，类型是否能匹配
 * 30. 交换机分离，一个交换机只干一个事
 * 31. 更新用户token的操作放到工具类中
 * 32. api请求文档
 * 33. 各种缓存新增前需要判断当前是否有默认值，有的话需要删除
 * 34. 用户登录时，推荐视频作者的关注状态
 * 35. 抛出自定义异常后，全局异常处理器返回的响应格式是否正确
 * 36. 部分请求量高的RabbitMQ消息改成Work模型，单一职责，并且能者多劳
 * todo: 37：看https://www.bilibili.com/video/BV11Z4y1f7cT，实现分布式事务
 * todo: 38：不再关注于业务，而是关注于优化
 * todo: 39：salt库怎么存储，因为要一个用户对应一个salt值，且需要分库存储
 * todo: 40：非法SQL拦截
 * 41：去掉代码中的循环依赖
 * todo: 42：视频相关表和用户相关表分开
 */

/*
 *
 * 　　┏┓　　　┏┓+ +
 * 　┏┛┻━━━┛┻┓ + +
 * 　┃　　　　　　　┃
 * 　┃　　　━　　　┃ ++ + + +
 *  ████━████ ┃+
 * 　┃　　　　　　　┃ +
 * 　┃　　　┻　　　┃
 * 　┃　　　　　　　┃ + +
 * 　┗━┓　　　┏━┛
 * 　　　┃　　　┃
 * 　　　┃　　　┃ + + + +
 * 　　　┃　　　┃
 * 　　　┃　　　┃ +  神兽保佑
 * 　　　┃　　　┃    代码无bug
 * 　　　┃　　　┃　　+
 * 　　　┃　 　　┗━━━┓ + +
 * 　　　┃ 　　　　　　　┣┓
 * 　　　┃ 　　　　　　　┏┛
 * 　　　┗┓┓┏━┳┓┏┛ + + + +
 * 　　　　┃┫┫　┃┫┫
 * 　　　　┗┻┛　┗┻┛+ + + +
 */
