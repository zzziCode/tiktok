package com.zzzi.videoservice.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.zzzi.common.result.CommonVO;
import com.zzzi.common.result.VideoFeedListVO;
import com.zzzi.common.result.VideoListVO;
import com.zzzi.common.result.VideoVO;
import com.zzzi.videoservice.dto.VideoFeedDTO;
import com.zzzi.videoservice.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    @SentinelResource("userWorks")
    @GetMapping("/publish/list")
    public VideoListVO getPublishList(String token, Long user_id) {
        log.info("获取用户投稿列表,token为：{}，user_id为：{}", token, user_id);
        //截取真正的token，去掉前缀"login:token:"
        if (token.startsWith("login:token:"))
            token = token.substring(12);
        List<VideoVO> videoVOList = videoService.getPublishListByAuthorId(token, user_id);
        if (videoVOList == null)
            return VideoListVO.fail("用户没有作品");
        return VideoListVO.success("成功", videoVOList);
    }

    /**
     * @author zzzi
     * @date 2024/3/29 12:14
     * 视频的推流，每次推流三十个视频
     * 如果没有传递latest_time，就按照当前时间推荐30个，并返回当前推荐视频的最早时间
     * 30个视频刷完按照这个最早时间继续推荐
     * <p>
     * 没传latest_time，默认为最新时间
     * 没传token，就不判断当前用户和视频用户的关注关系和视频的点赞关系
     */
    @GetMapping("/feed")
    public VideoFeedListVO getFeedList(@RequestParam(required = false) Long latest_time,
                                       @RequestParam(required = false) String token) {
        log.info("视频推荐,token为 :{}", token);
        //截取真正的token，去掉前缀"login:token:"
        if (token != null && token.startsWith("login:token:"))
            token = token.substring(12);
        /**@author zzzi
         * @date 2024/4/2 16:50
         * 时间没传，默认从当前时间向前推荐
         */
        if (latest_time == null)
            latest_time = System.currentTimeMillis();
        //默认下次也从当前时间开始推荐，这样视频少的时候可以循环推荐
        VideoFeedDTO videoFeedDTO = videoService.getFeedList(latest_time, token);
        if (videoFeedDTO != null) {
            List<VideoVO> videoVOList = videoFeedDTO.getFeed_list();

            //更新下次推荐时间
            Long next_time = videoFeedDTO.getNext_time();
            return VideoFeedListVO.success("获取推荐视频成功", next_time, videoVOList);
        }
        return VideoFeedListVO.fail("获取推荐视频失败");
    }

    /**
     * @author zzzi
     * @date 2024/3/27 14:44
     * 用户投稿视频
     * 可以根据用户的token解析出用户的userId
     */
    @PostMapping("/publish/action")
    public CommonVO postVideo(MultipartFile data, String token, String title) {
        log.info("用户投稿,token 为：{}", token);
        //截取真正的token，去掉前缀"login:token:"
        if (token.startsWith("login:token:"))
            token = token.substring(12);
        videoService.postVideo(data, token, title);

        //只要不出错误，说明成功投稿
        return CommonVO.success("投稿成功");
    }
}
