package com.zzzi.userservice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzzi
 * @date 2024/3/29 16:15
 * 用户关注实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_follows")
public class UserFollowDO extends Model<UserFollowDO> {
    @TableId
    private Long followId;
    //谁点了关注
    private Long followerId;
    //被关注者是谁
    private Long followedId;
}
