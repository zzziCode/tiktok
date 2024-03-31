package com.zzzi.videoservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzzi.common.result.CommonVO;
import com.zzzi.common.result.UserVO;
import com.zzzi.videoservice.entity.VideoDO;
import com.zzzi.videoservice.result.VideoListVO;
import com.zzzi.videoservice.result.VideoVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface VideoService extends IService<VideoDO> {
    void postVideo(MultipartFile data, String token, String title);

    List<VideoVO> getPublishListByAuthorId(String token, Long user_id);

    List<VideoVO> getFeedList(Long latest_time, String token);

    VideoDO upload(Long authorId, MultipartFile data, String title);

    List<VideoVO> packageVideoListVO(List<String> userWorkList, UserVO userVO);

    List<VideoVO> rebuildUserWorkListCache(Long user_id, UserVO userVO);

    VideoVO packageVideoVO(VideoDO videoDO, UserVO userVO);
}
