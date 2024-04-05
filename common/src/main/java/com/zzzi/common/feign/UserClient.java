package com.zzzi.common.feign;


import com.zzzi.common.feign.fallback.UserClientFallbackFactory;
import com.zzzi.common.result.UserInfoVO;
import com.zzzi.common.result.UserRelationListVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

//在这里指定好对应的微服务，之后就可以自动帮忙远程调用
//要注意返回值和参数类型都必须一致

/**
 * @author zzzi
 * @date 2024/4/2 18:17
 * Get请求必须添加@RequestParam，否则会报错Method has too many Body parameters
 * 因为识别不到远程调用传递来的参数
 */
@FeignClient(value = "userservice", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {
    //获取用户信息
    @GetMapping("/douyin/user/")
    UserInfoVO userInfo(@RequestParam("user_id") Long authorId);

    //获取用户关注列表
    @GetMapping("/douyin/relation/follow/list/")
    UserRelationListVO getFollowList(@RequestParam("user_id") String user_id,
                                     @RequestParam("token") String token);
}
