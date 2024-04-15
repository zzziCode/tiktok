package com.zzzi.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzzi.common.result.ValidCodeVO;
import com.zzzi.userservice.dto.UserDTO;
import com.zzzi.userservice.entity.UserDO;
import com.zzzi.common.result.UserVO;

public interface UserService extends IService<UserDO> {
    UserDTO loginWithPassWord(String username, String password);

    UserDTO register(String username, String password);

    UserVO getUserInfo(String user_id);

    UserDTO loginWithValidCode(String phoneNum, String validCode);

    String getValidCode(String phoneNum);

}
