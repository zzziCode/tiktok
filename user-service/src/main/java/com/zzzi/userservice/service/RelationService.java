package com.zzzi.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzzi.common.result.UserVO;
import com.zzzi.userservice.entity.UserFollowDO;

import java.util.List;

public interface RelationService extends IService<UserFollowDO> {
    void followAction(String token, Long to_user_id);

    void followUnAction(String token, Long to_user_id1);

    List<UserVO> getFollowList(String user_id, String token);

    List<UserVO> getFollowerList(String user_id, String token);

    List<UserVO> getFriendList(String user_id, String token);
}
