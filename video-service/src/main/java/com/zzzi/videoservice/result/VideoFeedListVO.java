package com.zzzi.videoservice.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoFeedListVO {
    private Integer status_code;
    private String status_msg;
    /**@author zzzi
     * @date 2024/3/29 12:20
     * 返回结果时告知下次推荐视频的时间节点
     * 防止刷到旧视频
     */
    private Long next_time;
    List<VideoVO> video_list;
}
