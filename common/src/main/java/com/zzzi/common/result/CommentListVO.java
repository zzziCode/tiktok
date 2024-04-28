package com.zzzi.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author zzzi
 * @date 2024/4/2 22:22
 * 视频评论操作返回的实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentListVO {
    private Integer status_code;
    private String status_msg;
    private List<CommentVO> comment_list;

    public static CommentListVO success(String status_msg, List<CommentVO> comment_list) {
        return new CommentListVO(0, status_msg, comment_list);
    }

    public static CommentListVO fail(String status_msg) {
        return new CommentListVO(-1, status_msg, null);
    }
}
