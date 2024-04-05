package com.zzzi.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzzi
 * @date 2024/3/26 21:17
 * 用户注册和登录返回这个VO对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterLoginVO {
    //状态码，0-成功，其他值-失败
    private Integer status_code;
    //状态信息
    private String status_msg;
    //用户id
    private Long user_id;
    //用户注册之后生成的token
    private String token;

    /**
     * @author zzzi
     * @date 2024/3/26 21:23
     * 向前端返回结果时调用这个函数即可
     */
    public static UserRegisterLoginVO success(Long user_id, String token) {
        return new UserRegisterLoginVO(0, "成功", user_id, token);
    }

    public static UserRegisterLoginVO fail(String status_msg) {
        return new UserRegisterLoginVO(-1, status_msg, null, null);
    }
}
