package com.zzzi.videoservice.listener;

import ch.qos.logback.core.joran.conditional.ThenOrElseActionBase;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.exception.CommentActionException;
import com.zzzi.videoservice.entity.VideoDO;
import com.zzzi.videoservice.mapper.VideoMapper;
import com.zzzi.common.utils.UpdateVideoInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
public class CommentListener {

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private UpdateVideoInfoUtils updateVideoInfoUtils;
    @Autowired
    private Gson gson;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = "direct.comment"),
                    exchange = @Exchange(name = RabbitMQKeys.COMMENT_EXCHANGE, type = ExchangeTypes.DIRECT),
                    key = {RabbitMQKeys.COMMENT_KEY}
            )
    )
    @Transactional
    public void listenToComment(@Payload long videoId) {
        log.info("监听到用户评论操作");

        //更新视频评论数
        VideoDO videoDO = videoMapper.selectById(videoId);
        Integer commentCount = videoDO.getCommentCount();
        //加上乐观锁
        LambdaQueryWrapper<VideoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoDO::getCommentCount, commentCount);
        videoDO.setCommentCount(commentCount + 1);
        int update = videoMapper.update(videoDO, queryWrapper);
        //更新失败说明出现线程安全问题，此时评论失败
        if (update != 1) {
            CommentListener commentListener = (CommentListener) AopContext.currentProxy();
            commentListener.listenToComment(videoId);
        }

        //更新视频缓存
        String videoDOJson = gson.toJson(videoDO);
        updateVideoInfoUtils.updateVideoInfoCache(videoId, videoDOJson);
    }
}
