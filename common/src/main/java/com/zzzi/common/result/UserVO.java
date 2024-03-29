package com.zzzi.common.result;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**@author zzzi
 * @date 2024/3/28 14:28
 * 这个类型多个模块都能用到，于是抽取到这里
 */
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;
    private String name;
    private Integer follow_count;
    private Integer follower_count;
    private boolean is_follow;
    //下面的字段是新增的
    private String avatar;
    private String background_image;
    private String signature;
    private Long total_favorited;
    private Integer work_count;
    private Integer favorite_count;


    /**
     * @author zzzi
     * @date 2024/3/27 13:33
     * 当某些字段Json转换出现问题时，主要原因就是set和get方法设置出现问题
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getFollow_count() {
        return follow_count;
    }

    public void setFollow_count(Integer follow_count) {
        this.follow_count = follow_count;
    }

    public Integer getFollower_count() {
        return follower_count;
    }

    public void setFollower_count(Integer follower_count) {
        this.follower_count = follower_count;
    }

    public boolean getIs_follow() {
        return is_follow;
    }

    public void setIs_follow(boolean is_follow) {
        this.is_follow = is_follow;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getBackground_image() {
        return background_image;
    }

    public void setBackground_image(String background_image) {
        this.background_image = background_image;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Long getTotal_favorited() {
        return total_favorited;
    }

    public void setTotal_favorited(Long total_favorited) {
        this.total_favorited = total_favorited;
    }

    public Integer getWork_count() {
        return work_count;
    }

    public void setWork_count(Integer work_count) {
        this.work_count = work_count;
    }

    public Integer getFavorite_count() {
        return favorite_count;
    }

    public void setFavorite_count(Integer favorite_count) {
        this.favorite_count = favorite_count;
    }
}
