package com.zzzi.common.result;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//每一条消息对应的返回实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {

    private Long id;
    //消息具体内容
    private String content;
    //这个封装的时候应该获取的是时间的毫秒值
    private Long create_time;
}
