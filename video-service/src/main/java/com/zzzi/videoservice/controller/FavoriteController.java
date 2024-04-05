package com.zzzi.videoservice.controller;

import com.zzzi.common.result.CommonVO;
import com.zzzi.common.result.VideoListVO;
import com.zzzi.common.result.VideoVO;
import com.zzzi.videoservice.service.FavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//用户点赞的相关操作
@RestController
@RequestMapping("/douyin/favorite")
@Slf4j
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * @author zzzi
     * @date 2024/4/2 12:50
     * 用户点赞/取消点赞
     * 失败时会在service层报错
     */
    @PostMapping("/action")
    public CommonVO favoriteAction(String token, String video_id, String action_type) {
        log.info("用户点赞操作service，token为：{}，video_id为：{},action_type为：{}", token, video_id, action_type);
        //截取真正的token
        if (token.startsWith("login:token:"))
            token = token.substring(12);
        String status_msg = "";
        if ("1".equals(action_type)) {
            log.info("用户点赞");
            favoriteService.favoriteAction(token, video_id);
            status_msg = "成功点赞";
        } else {
            log.info("用户取消点赞");
            favoriteService.favoriteUnAction(token, video_id);
            status_msg = "成功取消点赞";
        }
        return CommonVO.success(status_msg);
    }

    /**
     * @author zzzi
     * @date 2024/4/2 12:50
     * 获取用户喜欢列表
     */
    @GetMapping("/list")
    public VideoListVO getFavoriteList(String user_id, String token) {
        log.info("获取用户点赞列表,user_id为：{}，token为：{}", user_id, token);
        //截取所有的token
        if (token != null && token.startsWith("login:token:"))
            token = token.substring(12);
        List<VideoVO> videoVOList = favoriteService.getFavoriteList(user_id, token);
        if (videoVOList != null) {
            return VideoListVO.success("成功", videoVOList);
        }
        return VideoListVO.fail("获取用户点赞列表失败");
    }
}
