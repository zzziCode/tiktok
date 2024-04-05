package com.zzzi.videoservice.entity;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//用户关注实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("favorite")
public class FavoriteDO extends Model<FavoriteDO> {
    @TableId
    private Long favoriteId;
    //谁点赞
    private Long userId;
    //哪个视频被点赞
    private Long videoId;
}
