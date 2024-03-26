package com.zzzi.videoservice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("video")
public class VideoDO {
    @TableId
    private Long videoId;

    private Long authorId;

    private String coverUrl;

    private String playUrl;

    private Date createTime;

    private Date updateTime;

    private String title;

    private Integer favoriteCount;

    private Integer commentCount;

}

