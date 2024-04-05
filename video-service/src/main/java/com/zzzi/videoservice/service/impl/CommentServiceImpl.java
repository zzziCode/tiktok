package com.zzzi.videoservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.constant.RedisDefaultValue;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.CommentActionException;
import com.zzzi.common.exception.CommentListException;
import com.zzzi.common.feign.UserClient;
import com.zzzi.common.result.CommentVO;
import com.zzzi.common.result.CommonVO;
import com.zzzi.common.result.UserVO;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.common.utils.RandomUtils;
import com.zzzi.videoservice.entity.CommentDO;
import com.zzzi.videoservice.mapper.CommentMapper;
import com.zzzi.videoservice.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.indexer.HalfArrayIndexer;
import org.ietf.jgss.GSSName;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.nodes.CollectionNode;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author zzzi
 * @date 2024/4/3 22:13
 * 关于评论模块的全部操作
 */
@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, CommentDO> implements CommentService {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private UserClient userClient;
    @Value("${video_comment_max_size}")
    public Long VIDEO_COMMENT_MAX_SIZE;

    /**
     * @author zzzi
     * @date 2024/4/4 12:36
     * 视频评论操作
     */
    @Override
    @Transactional
    public CommentVO commentAction(String token, String video_id, String comment_text) {
        log.info("用户评论操作service，token为：{}，video_id为：{}", token, video_id);
        //解析出用户的id
        Long userId = JwtUtils.getUserIdByToken(token);

        CommentDO commentDO = new CommentDO();
        commentDO.setUserId(userId);
        commentDO.setVideoId(Long.valueOf(video_id));
        commentDO.setCommentText(comment_text);

        //评论表新增
        int insert = commentMapper.insert(commentDO);
        if (insert != 1) {//新增失败
            throw new CommentActionException("用户评论失败");
        }

        //视频评论缓存新增
        List<String> comments = redisTemplate.opsForList().range(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id, 0, -1);
        //有默认值先删除
        if (comments.contains(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
            redisTemplate.delete(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id);
        }

        //将视频评论加入缓存中
        Gson gson = new Gson();
        String commentDOJson = gson.toJson(commentDO);
        redisTemplate.opsForList().leftPush(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id, commentDOJson);
        //视频的评论数过多，此时删除很早的评论
        while (redisTemplate.opsForList().size(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id) > VIDEO_COMMENT_MAX_SIZE) {
            redisTemplate.opsForList().rightPop(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id);
        }
        //发送异步消息，更新视频表的信息和缓存
        rabbitTemplate.convertAndSend(RabbitMQKeys.COMMENT_EXCHANGE, RabbitMQKeys.COMMENT_KEY, Long.valueOf(video_id));

        //返回包装的结果
        //远程调用获得当前评论用户的基本信息
        UserVO user = userClient.userInfo(userId).getUser();
        return packageCommentVO(commentDO, user);
    }


    @Override
    @Transactional
    public CommentVO commentUnAction(String token, String video_id, String comment_id) {
        log.info("用户删除评论操作service，token为：{}，video_id为：{},comment_id为：{}", token, video_id, comment_id);
        //解析出用户的id
        Long userId = JwtUtils.getUserIdByToken(token);
        //删除对应的评论
        CommentDO commentDO = commentMapper.selectById(comment_id);
        int delete = commentMapper.deleteById(commentDO);
        if (delete != 1) {
            throw new CommentActionException("用户删除评论失败");
        }

        //删除评论缓存
        redisTemplate.delete(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id);
        //异步更新视频的评论数，包括数据库和缓存
        rabbitTemplate.convertAndSend(RabbitMQKeys.COMMENT_EXCHANGE, RabbitMQKeys.UN_COMMENT_KEY, Long.valueOf(video_id));
        //返回当前被删除的评论
        //远程调用获得当前评论用户的基本信息
        UserVO user = userClient.userInfo(userId).getUser();
        return packageCommentVO(commentDO, user);
    }

    @Override
    public List<CommentVO> getCommentList(String token, String video_id) {
        log.info("用户评论列表service，token为：{}，video_id为：{}", token, video_id);
        //先得到当前视频的所有评论信息
        //解析出用户的id
        Long userId = null;
        if (token != null && !"".equals(token)) {
            userId = JwtUtils.getUserIdByToken(token);
        }

        //先从缓存中获取到所有的评论信息
        List<String> commentList = redisTemplate.opsForList().range(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id, 0, -1);
        if (commentList != null && !commentList.isEmpty()) {
            return packageCommentListVO(commentList, userId, token);
        } else {//缓存中没有
            try {
                //1. 先尝试获取互斥锁，没获取到一直尝试，互斥锁的key为用户作品列表的key
                long currentThreadId = Thread.currentThread().getId();
                Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id + "_mutex", currentThreadId + "");
                if (!absent) {
                    Thread.sleep(50);
                    CommentService commentService = (CommentService) AopContext.currentProxy();
                    commentService.getCommentList(token, video_id);
                }
                //获取到了互斥锁，再次判断缓存中是不是有
                commentList = redisTemplate.opsForList().range(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id, 0, -1);
                if (commentList != null && !commentList.isEmpty()) {
                    return packageCommentListVO(commentList, userId, token);
                }
                //在这里就是真的没有，此时缓存重建
                return rebuildCommentListCache(video_id, userId, token);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CommentListException("获取当前视频评论列表失败");
            } finally {
                //最后需要删除互斥锁
                String currentThreadId = Thread.currentThread().getId() + "";
                String threadId = redisTemplate.opsForValue().get(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id + "_mutex");
                //加锁的就是当前线程才解锁
                if (threadId.equals(currentThreadId)) {
                    redisTemplate.delete(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id + "_mutex");
                }
            }
        }

    }

    /**
     * @author zzzi
     * @date 2024/4/4 13:26
     * 重建视频评论缓存，直接从数据库中查询
     */
    private List<CommentVO> rebuildCommentListCache(String video_id, Long userId, String token) {
        log.info("重建视频评论缓存service，token为：{}，video_id为：{},userId为：{}", token, video_id, userId);
        LambdaQueryWrapper<CommentDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentDO::getVideoId, video_id);
        List<CommentDO> commentDOList = commentMapper.selectList(queryWrapper);

        List<String> commentList = new ArrayList<>();
        //数据库中没有数据
        if (commentDOList == null || commentDOList.size() == 0) {
            commentList.add(RedisDefaultValue.REDIS_DEFAULT_VALUE);
            /**@author zzzi
             * @date 2024/4/4 13:29
             * 保存防止缓存穿透的默认值
             */
            redisTemplate.opsForList().leftPush(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id, RedisDefaultValue.REDIS_DEFAULT_VALUE);
            redisTemplate.expire(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id, 5, TimeUnit.MINUTES);
        } else {

            //删除可能存在的默认值
            redisTemplate.delete(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id);
            Gson gson = new Gson();
            for (CommentDO commentDO : commentDOList) {
                //将视频详细信息转换成Json
                String commentDOJson = gson.toJson(commentDO);
                commentList.add(commentDOJson);
            }
            redisTemplate.opsForList().leftPushAll(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id, commentList);
            //视频的评论数过多，此时删除很早的评论
            while (redisTemplate.opsForList().size(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id) > VIDEO_COMMENT_MAX_SIZE) {
                redisTemplate.opsForList().rightPop(RedisKeys.VIDEO_COMMENTS_PREFIX + video_id);
            }
        }
        return packageCommentListVO(commentList, userId, token);

    }

    /**
     * @author zzzi
     * @date 2024/4/4 13:21
     * 打包所有的视频评论列表
     * 针对每一个视频的评论者，判断与当前userId的关注关系
     */
    private List<CommentVO> packageCommentListVO(List<String> commentList, Long userId, String token) {
        log.info("打包评论列表service,userId为：{}，token为：{}", userId, token);
        //获取当前userId对应的关注列表
        List<UserVO> followList = null;
        if (token != null && !"".equals(token))
            followList = userClient.getFollowList(userId.toString(), token).getUser_list();
        Gson gson = new Gson();
        List<CommentVO> comment_list = new ArrayList<>();
        Map<Long, UserVO> userVOMap = new HashMap<>();
        //先判断是不是缓存的默认值
        if (comment_list.contains(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
            return null;
        }
        //在这里说明是真的有
        for (String commentJson : commentList) {
            CommentDO commentDO = gson.fromJson(commentJson, CommentDO.class);
            //获取当前评论者的id
            Long commentDOUserId = commentDO.getUserId();
            //当前Map中有，可以直接复用
            UserVO userVO;
            if (userVOMap.containsKey(commentDOUserId)) {
                userVO = userVOMap.get(commentDOUserId);
            } else {//Map中没有，需要远程调用获取，并且判断关注关系
                userVO = userClient.userInfo(commentDOUserId).getUser();
                //已经登录的用户查看评论列表，已经关注时要设置关注状态
                if (token != null && followList != null && followList.contains(userVO)) {
                    userVO.setIs_follow(true);
                }
                //将其保存到Map中便于后期复用
                userVOMap.put(commentDOUserId, userVO);
            }
            //有了commentDO和userVo之后，打包成commentVO
            CommentVO commentVO = packageCommentVO(commentDO, userVO);
            comment_list.add(commentVO);
        }
        return comment_list;
    }

    /**
     * @author zzzi
     * @date 2024/4/4 12:51
     * 打包评论实体类返回，主要是评论创建时间的格式：MM-dd
     */
    private CommentVO packageCommentVO(CommentDO commentDO, UserVO user) {

        Date createTime = commentDO.getCreateTime();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
        String create_date = sdf.format(createTime);
        CommentVO commentVO = new CommentVO();
        commentVO.setId(commentDO.getCommentId());
        commentVO.setUser(user);
        commentVO.setContent(commentDO.getCommentText());
        commentVO.setCreate_date(create_date);

        return commentVO;
    }
}
