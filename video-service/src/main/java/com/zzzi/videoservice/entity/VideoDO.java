package com.zzzi.videoservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("video")
public class VideoDO extends Model<VideoDO> {
    @TableId
    private Long videoId;

    private Long authorId;

    private String coverUrl;

    private String playUrl;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(fill = FieldFill.INSERT)
    private String title;

    @TableField(fill = FieldFill.INSERT)
    private Integer favoriteCount;

    @TableField(fill = FieldFill.INSERT)
    private Integer commentCount;

}

