package com.zzzi.userservice.config;

import com.zzzi.userservice.interceptor.LoginUserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @author zzzi
 * @date 2024/3/29 15:23
 * 给当前项目配置拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 只拦截需要登录校验的请求.比如登录/评论/点赞/发布视频/用户关注/推送视频
        // 除了登录和获取资源还有推送视频不拦截，其他的都拦截
        registry.addInterceptor(new LoginUserInterceptor()).excludePathPatterns(
                "/douyin/user/**",
                "/resource/**",
                "/douyin/feed/**"
        ).order(1);
    }

}
