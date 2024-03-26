package com.zzzi.userservice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@TableName("users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDO {

    @TableId
    private Long userId;
    private String username;
    private String email;
    private String password;
    private Date createTime;
    private Date updateTime;
    private Integer followCount;
    private Integer followerCount;
    private String avatar;
    private String backgroundImage;
    private String signature;
    private Long totalFavorited;
    private Integer workCount;
    private Integer favoriteCount;

    /**@author zzzi
     * @date 2024/3/25 16:12
     * 为了缓存用户登录信息时使用
     * 用户登录缓存只缓存不变信息
     */
    public UserDO(Long userId, String username, String email, String password, Date createTime, Date updateTime) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

}
