package com.zzzi.videoservice.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.exception.VideoException;
import com.zzzi.videoservice.entity.VideoDO;
import com.zzzi.videoservice.mapper.VideoMapper;
import com.zzzi.common.utils.UpdateVideoInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author zzzi
 * @date 2024/3/29 16:38
 * 在这里异步的更新视频的基本信息
 */
@Service
@Slf4j
public class FavoriteListenerTwo {
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private UpdateVideoInfoUtils updateVideoInfoUtils;
    @Autowired
    private Gson gson;

    @RabbitListener(queues = {RabbitMQKeys.FAVORITE_VIDEO})
    @Transactional
    public void listenToFavorite(@Payload long videoId) {
        log.info("第二个消费者监听到用户点赞操作，更新视频信息");

        //更新视频点赞数
        VideoDO videoDO = videoMapper.selectById(videoId);
        Integer favoriteCount = videoDO.getFavoriteCount();
        LambdaQueryWrapper<VideoDO> queryWrapper = new LambdaQueryWrapper<>();
        //加上乐观锁
        queryWrapper.eq(VideoDO::getFavoriteCount, favoriteCount);
        videoDO.setFavoriteCount(favoriteCount + 1);
        int update = videoMapper.update(videoDO, queryWrapper);
        if (update != 1) {
            //手动实现CAS算法
            FavoriteListenerTwo favoriteListener = (FavoriteListenerTwo) AopContext.currentProxy();
            favoriteListener.listenToFavorite(videoId);
        }

        //更新视频缓存
        String videoDOJson = gson.toJson(videoDO);
        updateVideoInfoUtils.updateVideoInfoCache(videoId, videoDOJson);
    }
}
