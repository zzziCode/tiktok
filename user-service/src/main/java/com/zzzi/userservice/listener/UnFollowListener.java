package com.zzzi.userservice.listener;

import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.userservice.entity.UserDO;
import com.zzzi.userservice.entity.UserFollowDO;
import com.zzzi.userservice.mapper.UserMapper;
import org.springframework.amqp.core.ExchangeTypes;
import com.zzzi.userservice.utils.UpdateUserInfoUtils;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**@author zzzi
 * @date 2024/3/29 16:59
 * 这里执行取消关注的逻辑
 */
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
                    value = @Queue(name = "direct.follow"),
                    exchange = @Exchange(name = RabbitMQKeys.EXCHANGE_NAME, type = ExchangeTypes.DIRECT),
                    key = {RabbitMQKeys.UN_FOLLOW_KEY}
            )
    )
    @Transactional
    public void listenToUnFollow(String userFollowDOJson) {
        //将接收到的实体转换成实体类
        Gson gson = new Gson();
        UserFollowDO userFollowDO = gson.fromJson(userFollowDOJson, UserFollowDO.class);

        //得到关注者和被关注者的id
        Long followerId = userFollowDO.getFollowerId();
        Long followedId = userFollowDO.getFollowedId();

        //查询更新两个用户的信息表和缓存
        //得到被关注者的信息
        UserDO followed = userMapper.selectById(followedId);
        //得到关注者的信息
        UserDO follower = userMapper.selectById(followerId);

        //更新关注者的关注数
        Integer followerCount = follower.getFollowCount();
        follower.setFollowCount(followerCount - 1);
        userMapper.updateById(follower);

        //更新被关注者的粉丝数
        Integer followedCount = followed.getFollowerCount();
        followed.setFollowerCount(followedCount - 1);
        userMapper.updateById(followed);

        //调用方法更新用户缓存
        String followerJson = gson.toJson(follower);
        String followedJson = gson.toJson(followed);

        updateUserInfoUtils.updateUserInfoCache(followerId,followerJson);
        updateUserInfoUtils.updateUserInfoCache(followedId,followedJson);
    }
}
