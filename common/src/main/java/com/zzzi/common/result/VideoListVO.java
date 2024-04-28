package com.zzzi.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bytedeco.javacpp.annotation.NoDeallocator;

import java.util.List;

@Data
@NoDeallocator
@AllArgsConstructor
public class VideoListVO {
    private Integer status_code;
    private String status_msg;

    List<VideoVO> video_list;

    //成功调用这个函数
    public static VideoListVO success(String status_msg, List<VideoVO> video_list) {
        return new VideoListVO(0, status_msg, video_list);
    }

    //失败调用这个函数
    public static VideoListVO fail(String status_msg) {
        return new VideoListVO(-1, status_msg, null);
    }
}
