package com.zzzi.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author zzzi
 * @date 2024/3/29 22:16
 * 在这里定义用户关注列表、粉丝列表、好友列表的返回结果实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRelationListVO {
    //状态码，0-成功，其他值-失败
    private Integer status_code;
    //状态信息
    private String status_msg;
    //在这里封装用户列表
    List<UserVO> user_list;

    //成功返回这个结果
    //根据返回的字符串判断获取的是关注列表、粉丝列表还是好友列表
    public static UserRelationListVO success(String status_msg, List<UserVO> user_list) {
        return new UserRelationListVO(0, status_msg, user_list);
    }

    //失败调用这个函数
    public static UserRelationListVO fail(String status_msg) {
        return new UserRelationListVO(-1, status_msg, null);
    }
}
