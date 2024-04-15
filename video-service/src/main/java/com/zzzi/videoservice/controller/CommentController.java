package com.zzzi.videoservice.controller;

import com.zzzi.common.result.CommentActionVO;
import com.zzzi.common.result.CommentListVO;
import com.zzzi.common.result.CommentVO;
import com.zzzi.videoservice.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//评论模块
@RestController
@RequestMapping("/douyin/comment")
@Slf4j
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * @author zzzi
     * @date 2024/4/2 22:16
     * 用户评论操作
     */
    @PostMapping("/action")
    public CommentActionVO commentAction(String token, String video_id, String action_type,
                                         @RequestParam(required = false) String comment_text,
                                         @RequestParam(required = false) String comment_id) {
        log.info("用户评论操作,token 为：{}，video_id为：{}，action_type为：{}，comment_id为：{}", token, video_id, action_type, comment_id);
        //截取真正的token
        if (token.startsWith("login:token:"))
            token = token.substring(12);
        if ("1".equals(action_type)) {
            CommentVO commentVO = commentService.commentAction(token, video_id, comment_text);
            return commentVO != null ? CommentActionVO.success("评论成功", commentVO) :
                    CommentActionVO.fail("评论失败");
        } else {
            CommentVO commentVO = commentService.commentUnAction(token, video_id, comment_id);
            return commentVO != null ? CommentActionVO.success("删除评论成功", commentVO) :
                    CommentActionVO.fail("删除评论失败");
        }
    }

    /**
     * @author zzzi
     * @date 2024/4/2 22:17
     * 获取视频评论列表
     * 这里的token应该不是必须的，没有登录的时候看视频评论，用户is_follow默认都是false，视频也没有点赞
     * 先当他是必须的
     */
    @GetMapping("/list")
    public CommentListVO getCommentList(String token, String video_id) {
        log.info("获取视频评论列表,token为：{}，video_id为：{}", token, video_id);
        //截取真正的token
        if (token != null && token.startsWith("login:token:"))
            token = token.substring(12);
        List<CommentVO> comment_list = commentService.getCommentList(token, video_id);
        if (comment_list != null) {
            return CommentListVO.success("获取评论列表成功", comment_list);
        }
        return CommentListVO.fail("获取评论列表失败");
    }
}
