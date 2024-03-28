package com.zzzi.common.feign;



import com.zzzi.common.feign.fallback.UserClientFallbackFactory;
import com.zzzi.common.result.UserInfoVO;
import com.zzzi.common.result.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

//在这里指定好对应的微服务，之后就可以自动帮忙远程调用
//要注意返回值和参数类型都必须一致
@FeignClient(value = "userservice", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/douyin/user/")
    UserInfoVO userInfo(@RequestParam("user_id") Long authorId);
}
