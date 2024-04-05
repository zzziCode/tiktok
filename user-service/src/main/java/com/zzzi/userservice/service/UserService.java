package com.zzzi.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzzi.userservice.dto.UserDTO;
import com.zzzi.userservice.entity.UserDO;
import com.zzzi.common.result.UserVO;

public interface UserService extends IService<UserDO> {
    UserDTO login(String username, String password);

    UserDTO register(String username, String password);

    UserVO getUserInfo(String user_id);

}
