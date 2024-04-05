package com.zzzi.userservice.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.result.CommonVO;
import com.zzzi.common.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author zzzi
 * @date 2024/3/29 14:52
 * 不需要拦截的请求直接放行
 */
@Component
@Slf4j
public class LoginUserInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行无需登录的请求
        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();// 匹配器
        boolean register = antPathMatcher.match("/douyin/user/register/**", uri);// 注册
        boolean login = antPathMatcher.match("/douyin/user/login/**", uri);// 登录
        log.info("拦截请求：" + uri);
        //放行无需登录的请求
        if (register || login) {
            return true;
        }

        // 验证登录状态
        /**@author zzzi
         * @date 2024/3/29 14:53
         * 直接根据缓存中是否存在用户的token来判断
         * todo: 为了调试方便，先全部放行
         */
        //String token = request.getParameter("token");
        ////没有抛异常的话就是验签成功
        //Long userId = JwtUtils.getUserIdByToken(token);
        //String userToken = redisTemplate.opsForValue().get(RedisKeys.USER_TOKEN_PREFIX + userId);
        //if (userToken == null || "".equals(userToken)) {
        //    log.error("用户未登录，非法请求");
        //    CommonVO fail = CommonVO.fail("请先登录");
        //    String failString = JSONObject.toJSONString(fail);
        //    response.getWriter().write(failString);
        //    return false;
        //}
        return true;
    }
}
