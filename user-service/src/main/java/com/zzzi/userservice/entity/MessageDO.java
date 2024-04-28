package com.zzzi.userservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author zzzi
 * @date 2024/4/3 16:12
 * 消息对应的数据库实体类
 */
@TableName("message")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDO extends Model<MessageDO> {
    @TableId
    private Long messageId;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
