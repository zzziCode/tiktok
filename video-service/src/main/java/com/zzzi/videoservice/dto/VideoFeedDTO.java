package com.zzzi.videoservice.dto;

import com.zzzi.common.result.VideoVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoFeedDTO {

    private List<VideoVO> feed_list;
    private Long next_time;
}
