package com.zzzi.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzzi
 * @date 2024/4/14 20:53
 * 返回验证码的VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidCodeVO {

    private Integer status_code;
    private String status_msg;
    private String validCode;


    public static ValidCodeVO fail(String status_msg) {
        return new ValidCodeVO(-1, status_msg, null);
    }

    public static ValidCodeVO success(String validCode) {
        return new ValidCodeVO(0, "发送验证码成功", validCode);
    }
}
