package com.zzzi.common.result;

import com.zzzi.common.result.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    /**
     * @author zzzi
     * @date 2024/5/4 22:41
     * 前端判断isFather为true才解析son_list
     * 前端判断isFather为false才解析replyName
     */
    private boolean is_father;
    private List<CommentVO> son_list;
    private String replyName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreate_date() {
        return create_date;
    }

    public void setCreate_date(String create_date) {
        this.create_date = create_date;
    }

    public boolean getIs_father() {
        return is_father;
    }

    public void setIs_father(boolean father) {
        is_father = father;
    }

    public List<CommentVO> getSon_list() {
        return son_list;
    }

    public void setSon_list(List<CommentVO> son_list) {
        this.son_list = son_list;
    }

    public String getReplyName() {
        return replyName;
    }

    public void setReplyName(String replyName) {
        this.replyName = replyName;
    }
}
