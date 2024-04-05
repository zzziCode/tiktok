package com.zzzi.userservice.listener;

import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.userservice.entity.UserDO;
import com.zzzi.userservice.entity.UserFollowDO;
import com.zzzi.userservice.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import com.zzzi.common.utils.UpdateUserInfoUtils;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zzzi
 * @date 2024/3/29 16:59
 * 这里执行取消关注的逻辑
 */
@Service
@Slf4j
public class UnFollowListener {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UpdateUserInfoUtils updateUserInfoUtils;


    /**
     * @author zzzi
     * @date 2024/3/29 16:49
     * 用户关注在这里更新双方的用户信息
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "direct.un_follow"),
                    exchange = @Exchange(name = RabbitMQKeys.FOLLOW_EXCHANGE, type = ExchangeTypes.DIRECT),
                    key = {RabbitMQKeys.UN_FOLLOW_KEY}
            )
    )
    @Transactional
    public void listenToUnFollow(String userUnFollowDOJson) {
        log.info("监听到用户取消关注");
        //将接收到的实体转换成实体类
        Gson gson = new Gson();
        UserFollowDO userUnFollowDO = gson.fromJson(userUnFollowDOJson, UserFollowDO.class);

        //得到取消关注者和被取消关注者的id
        Long unFollowerId = userUnFollowDO.getFollowerId();
        Long unFollowedId = userUnFollowDO.getFollowedId();

        //查询更新两个用户的信息表和缓存
        //得到被取消关注者的信息
        UserDO unFollowed = userMapper.selectById(unFollowedId);
        //得到取消关注者的信息
        UserDO unFollower = userMapper.selectById(unFollowerId);

        //更新取消关注者的关注数
        Integer followerCount = unFollower.getFollowCount();
        unFollower.setFollowCount(followerCount - 1);
        userMapper.updateById(unFollower);

        //更新被取消关注者的粉丝数
        Integer followedCount = unFollowed.getFollowerCount();
        unFollowed.setFollowerCount(followedCount - 1);
        userMapper.updateById(unFollowed);

        //调用方法更新用户缓存
        String followerJson = gson.toJson(unFollower);//主动取消关注者
        String followedJson = gson.toJson(unFollowed);//被动取消关注者

        updateUserInfoUtils.updateUserInfoCache(unFollowerId, followerJson);
        updateUserInfoUtils.updateUserInfoCache(unFollowedId, followedJson);
    }
}
