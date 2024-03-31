package com.zzzi.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzzi
 * @date 2024/3/27 14:49
 * 通用的返回结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonVO {
    private Integer status_code;
    private String status_msg;

    public static CommonVO success(String status_msg) {
        return new CommonVO(0, status_msg);
    }

    public static CommonVO fail(String status_msg) {
        return new CommonVO(-1, status_msg);
    }
}
