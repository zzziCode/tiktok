package com.zzzi.common.feign.fallback;

import com.zzzi.common.feign.UserClient;
import com.zzzi.common.result.UserInfoVO;
import com.zzzi.common.result.UserRelationListVO;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    //这里指定userClient访问失败时应该怎么做
    //这里返回了一个空的信息
    @Override
    public UserClient create(Throwable throwable) {
        return new UserClient() {

            @Override
            public UserInfoVO userInfo(Long id) {
                log.error("查询用户异常", throwable);
                return new UserInfoVO();
            }

            @Override
            public UserRelationListVO getFollowList(String user_id, String token) {
                log.error("查询关注列表异常", throwable);
                return new UserRelationListVO();
            }
        };
    }
}
