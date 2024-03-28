package com.zzzi.userservice.dto;


import com.zzzi.userservice.entity.UserDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**@author zzzi
 * @date 2024/3/26 21:27
 * 三层之间传递使用这个数据对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UserDO userDO;
    private String token;
}
