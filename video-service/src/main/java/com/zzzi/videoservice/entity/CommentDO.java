package com.zzzi.videoservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

//视频评论实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("comment")
public class CommentDO {
    @TableId
    private Long commentId;
    private Long userId;
    private String commentText;
    private Long videoId;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
