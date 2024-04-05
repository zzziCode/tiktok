package com.zzzi.common.result;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzzi
 * @date 2024/3/26 21:17
 * 查询用户所有信息时返回这个VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO {
    //状态码，0-成功，其他值-失败
    private Integer status_code;
    //状态信息
    private String status_msg;
    //用户全部信息
    private UserVO user;


    //成功调用这个函数
    public static UserInfoVO success(UserVO user) {
        return new UserInfoVO(0, "成功", user);
    }

    //失败调用这个函数
    public static UserInfoVO fail(String status_msg) {
        return new UserInfoVO(-1, status_msg, null);
    }
}
