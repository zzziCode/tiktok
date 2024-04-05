package com.zzzi.videoservice.listener;

import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.videoservice.entity.VideoDO;
import com.zzzi.videoservice.mapper.VideoMapper;
import com.zzzi.common.utils.UpdateVideoInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author zzzi
 * @date 2024/3/29 16:38
 * 在这里异步的更新用户的基本信息
 */
@Service
@Slf4j
public class UnFavoriteListenerOne {
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private UpdateVideoInfoUtils updateVideoInfoUtils;


    /**
     * @author zzzi
     * @date 2024/4/2 13:24
     * 在这里更新对应的视频信息
     */
    @RabbitListener(queues = {RabbitMQKeys.UN_FAVORITE_VIDEO})
    @Transactional
    public void listenToUnFavorite(@Payload long videoId) {
        log.info("第一个消费者监听到用户取消点赞操作");
        //更新视频点赞数
        VideoDO videoDO = videoMapper.selectById(videoId);
        Integer favoriteCount = videoDO.getFavoriteCount();
        videoDO.setFavoriteCount(favoriteCount - 1);
        videoMapper.updateById(videoDO);

        //更新视频缓存
        Gson gson = new Gson();
        String videoDOJson = gson.toJson(videoDO);
        updateVideoInfoUtils.updateVideoInfoCache(videoId, videoDOJson);
    }
}
