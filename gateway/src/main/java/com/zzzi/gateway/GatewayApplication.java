package com.zzzi.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}

/**@author zzzi
 * @date 2024/3/30 13:27
 * todo：1. 分布式事务Seata
 * todo：2. 防止缓存穿透，在哪使用布隆过滤器
 * 3. 防止缓存雪崩：过期值打散+Sentinel限流
 * todo：4. 推拉模式的视频推流
 * 5. 用户作品缓存只缓存一部分新的，也就是leftPush之后超过长度需要rightPop
 * 6. 登录校验（有token说明已登录）
 * todo：7. 用户签到
 * todo：8. 改进RabbitMQ，实现异步通信的可靠性
 * todo：9. 主从复制
 * todo：10. 本地视频获取路径
 * todo：11. 获取用户的关注列表
 * 12. 测试完善关注和取消关注
 * 13. 完善用户是否登录
 * todo：14. 测试try-catch是否吃掉回滚，也就是事务为什么失效，如何解决
 * 15. 注册之后直接调用接口获取用户基本信息
 * todo：16. 使用canal实现缓存和数据库中的数据一致性
 * 17. 先更新数据库再操作缓存，因为缓存速度快，尽可能减小数据不一致问题
 * 18. 分布式锁解锁问题，判断是不是当前线程来解锁（看原项目怎么写的）
 * 19. setIfAbsent的问题，用户信息应该是不过期的
 * 20. Redis默认值过期值为5分钟
 * todo: 21. 排除所有空指针
 * todo: 22. 注册成功就应该保存token
 */
