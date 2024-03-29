package com.zzzi.userservice.controller;

import com.zzzi.common.result.CommonVO;
import com.zzzi.common.result.UserVO;
import com.zzzi.userservice.service.UserFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/douyin/relation")
public class RelationController {
    @Autowired
    private UserFollowService userFollowService;

    /**
     * @author zzzi
     * @date 2024/3/29 16:17
     * 根据用户传递来的操作判断是关注还是取消关注
     */
    @PostMapping("/action")
    public CommonVO followAction(String token, Long to_user_id, String action_type) {
        String status_msg = "";
        if (action_type.equals("1")) {
            userFollowService.followAction(token, to_user_id);
            status_msg = "成功关注";
        } else {
            userFollowService.followUnAction(token, to_user_id);
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
    public void getFollowList(String user_id, String token) {
        List<UserVO> user_list = userFollowService.getFollowList(user_id, token);
    }

}
