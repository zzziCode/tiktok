package com.zzzi.common.result;

import com.zzzi.common.result.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {

    private Long id;
    //获取评论列表时，需要判断当前用户是不是我关注的
    private UserVO user;
    //评论内容
    private String content;
    //评论日期
    private String create_date;
}
