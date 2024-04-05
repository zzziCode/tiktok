package com.zzzi.common.result;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzzi
 * @date 2024/4/2 22:22
 * 用户评论操作返回的实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentActionVO {
    private Integer status_code;
    private String status_msg;
    //当前用户评论的实体
    private CommentVO comment;

    public static CommentActionVO success(String status_msg, CommentVO comment) {
        return new CommentActionVO(0, status_msg, comment);
    }

    public static CommentActionVO fail(String status_msg) {
        return new CommentActionVO(-1, status_msg, null);
    }
}
