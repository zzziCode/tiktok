package com.zzzi.userservice.controller;

import com.zzzi.common.result.CommonVO;
import com.zzzi.common.result.UserVO;
import com.zzzi.common.result.UserRelationListVO;
import com.zzzi.userservice.service.RelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/douyin/relation")
@Slf4j
public class RelationController {
    @Autowired
    private RelationService relationService;

    /**
     * @author zzzi
     * @date 2024/3/29 16:17
     * 根据用户传递来的操作判断是关注还是取消关注
     */
    @PostMapping("/action")
    public CommonVO followAction(String token, Long to_user_id, String action_type) {
        log.info("用户关注操作,,token为：{}，to_user_id为：{}", token, to_user_id);
        String status_msg = "";
        //截取真正的token，去掉前缀"login:token:"
        if (token.startsWith("login:token:"))
            token = token.substring(12);
        if ("1".equals(action_type)) {
            relationService.followAction(token, to_user_id);
            status_msg = "成功关注";
        } else {
            relationService.followUnAction(token, to_user_id);
            status_msg = "成功取消关注";
        }
        return CommonVO.success(status_msg);
    }

    /**
     * @author zzzi
     * @date 2024/3/29 22:12
     * 获取当前用户的所有关注列表
     */
    @GetMapping("/follow/list")
    public UserRelationListVO getFollowList(String user_id, String token) {
        log.info("获取用户关注列表,token为：{}，user_id为：{}", token, user_id);
        //截取真正的token，去掉前缀"login:token:"
        if (token.startsWith("login:token:"))
            token = token.substring(12);
        List<UserVO> user_list = relationService.getFollowList(user_id, token);
        if (user_list == null || user_list.isEmpty()) {
            return UserRelationListVO.fail("用户关注列表为空");
        }
        return UserRelationListVO.success("获取关注列表成功", user_list);
    }

    /**
     * @author zzzi
     * @date 2024/4/1 12:40
     * 获取当前用户的所有粉丝列表
     */
    @GetMapping("/follower/list")
    public UserRelationListVO getFollowerList(String user_id, String token) {
        log.info("获取用户粉丝列表,token为：{}，user_id为：{}", token, user_id);
        //截取真正的token，去掉前缀"login:token:"
        if (token.startsWith("login:token:"))
            token = token.substring(12);
        List<UserVO> user_list = relationService.getFollowerList(user_id, token);
        if (user_list == null || user_list.isEmpty()) {
            return UserRelationListVO.fail("用户粉丝列表为空");
        }
        return UserRelationListVO.success("获取粉丝列表成功", user_list);
    }

    /**
     * @author zzzi
     * @date 2024/4/1 12:40
     * 获取当前用户所有的好友列表
     */
    @GetMapping("/friend/list")
    public UserRelationListVO getFriendList(String user_id, String token) {
        log.info("获取用户好友列表,token为：{}，user_id为：{}", token, user_id);
        //截取真正的token，去掉前缀"login:token:"
        if (token.startsWith("login:token:"))
            token = token.substring(12);
        List<UserVO> user_list = relationService.getFriendList(user_id, token);
        if (user_list == null || user_list.isEmpty()) {
            return UserRelationListVO.fail("用户好友列表为空");
        }
        return UserRelationListVO.success("获取好友列表成功", user_list);
    }

}
