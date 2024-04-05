package com.zzzi.common.result;

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
    /**
     * @author zzzi
     * @date 2024/3/29 12:20
     * 返回结果时告知下次推荐视频的时间节点
     * 防止刷到旧视频
     */
    private Long next_time;
    List<VideoVO> video_list;

    //成功调用这个函数
    public static VideoFeedListVO success(String status_msg, Long next_time, List<VideoVO> video_list) {
        return new VideoFeedListVO(0, status_msg, next_time, video_list);
    }

    //失败调用这个函数
    public static VideoFeedListVO fail(String status_msg) {
        return new VideoFeedListVO(-1, status_msg, null, null);
    }
}
