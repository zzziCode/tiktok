package com.zzzi.videoservice.controller;

import com.zzzi.common.result.CommonVO;
import com.zzzi.common.utils.UploadUtils;
import com.zzzi.common.utils.VideoUtils;
import com.zzzi.videoservice.entity.VideoDO;
import com.zzzi.videoservice.result.VideoListVO;
import com.zzzi.videoservice.result.VideoVO;
import com.zzzi.videoservice.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/douyin")
@Slf4j
public class VideoController {

    @Autowired
    private VideoService videoService;


    /**
     * @author zzzi
     * @date 2024/3/28 14:13
     * 获取用户的所有作品
     * 由于作品信息更新时会删除缓存，所以可能需要缓存重构
     * 并且作品列表涉及到获取用户信息，所以需要远程调用
     */
    @GetMapping("/publish/list")
    public VideoListVO getPublishList(String token, Long user_id) {
        List<VideoVO> videoVOList = videoService.getPublishListByAuthorId(token, user_id);
        if (videoVOList == null)
            return VideoListVO.fail("用户没有作品");
        return VideoListVO.success("成功", videoVOList);
    }

    @GetMapping("/feed")
    public void getFeedList(String latest_time, String token) {

    }

    /**
     * @author zzzi
     * @date 2024/3/27 14:44
     * 用户投稿视频
     * 可以根据用户的token解析出用户的userId
     */
    @PostMapping("/publish/action")
    public CommonVO postVideo(MultipartFile data, String token, String title) {
        videoService.postVideo(data, token, title);

        //只要不出错误，说明成功投稿
        return CommonVO.success("投稿成功");
    }
}
