package com.zzzi.videoservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzzi.common.result.UserVO;
import com.zzzi.videoservice.dto.VideoFeedDTO;
import com.zzzi.videoservice.entity.VideoDO;
import com.zzzi.common.result.VideoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface VideoService extends IService<VideoDO> {
    void postVideo(MultipartFile data, String token, String title);

    List<VideoVO> getPublishListByAuthorId(String token, Long user_id);

    VideoFeedDTO getFeedList(Long latest_time, String token);

    VideoDO getVideoInfo(String videoId);

    VideoVO packageVideoVO(VideoDO videoDO, UserVO userVO, String user_id, String token);

    VideoVO packageFavoriteVideoVO(VideoDO videoDO, UserVO userVO);
}
