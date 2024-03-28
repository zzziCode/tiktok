package com.zzzi.userservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author zzzi
 * @date 2024/3/26 21:17
 * User的数据库实体类
 */
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
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
    @TableField(fill = FieldFill.INSERT)
    private Integer followCount;
    @TableField(fill = FieldFill.INSERT)
    private Integer followerCount;
    @TableField(fill = FieldFill.INSERT)
    private String avatar;
    @TableField(fill = FieldFill.INSERT)
    private String backgroundImage;
    @TableField(fill = FieldFill.INSERT)
    private String signature;
    @TableField(fill = FieldFill.INSERT)
    private Long totalFavorited;
    @TableField(fill = FieldFill.INSERT)
    private Integer workCount;
    @TableField(fill = FieldFill.INSERT)
    private Integer favoriteCount;


    /**
     * @author zzzi
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
