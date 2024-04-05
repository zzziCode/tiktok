package com.zzzi.videoservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.constant.RedisDefaultValue;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.VideoListException;
import com.zzzi.common.feign.UserClient;
import com.zzzi.common.result.UserVO;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.common.utils.UpdateTokenUtils;
import com.zzzi.videoservice.entity.FavoriteDO;
import com.zzzi.videoservice.entity.VideoDO;
import com.zzzi.videoservice.mapper.FavoriteMapper;
import com.zzzi.videoservice.mapper.VideoMapper;
import com.zzzi.common.result.VideoVO;
import com.zzzi.videoservice.service.FavoriteService;
import com.zzzi.videoservice.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, FavoriteDO> implements FavoriteService {
    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private VideoService videoService;
    @Autowired
    private UserClient userClient;
    @Autowired
    private UpdateTokenUtils updateTokenUtils;

    /**
     * @author zzzi
     * @date 2024/4/2 12:54
     * 用户点赞
     * 同步操作
     * 1. 点赞表新增
     * 2. 用户点赞缓存新增
     * 异步操作，两个队列都监听这个消息
     * - 用户表更新，A的点赞数+1，B的获赞总数+1
     * - 用户信息缓存更新，A点赞数+1，B获赞总数+1
     * <p>
     * - 视频表更新：B的对应视频点赞数+1
     * - B的视频信息缓存更新，点赞数+1
     */
    @Override
    @Transactional
    public void favoriteAction(String token, String video_id) {
        log.info("用户点赞service");
        //获取点赞用户的id
        Long userId = JwtUtils.getUserIdByToken(token);
        Long videoId = Long.valueOf(video_id);
        FavoriteDO favoriteDO = new FavoriteDO();
        favoriteDO.setUserId(userId);
        favoriteDO.setVideoId(videoId);

        //点赞表新增
        int insert = favoriteMapper.insert(favoriteDO);
        //插入失败进行回滚
        if (insert != 1) {
            throw new RuntimeException("用户点赞失败");
        }

        //用户点赞作品缓存新增，新增之前是否需要删除
        try {
            Set<String> members = redisTemplate.opsForSet().members(RedisKeys.USER_FAVORITES_PREFIX + userId);
            //新增前判断是不是有默认值，有的话需要删除再添加
            if (members.contains(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
                redisTemplate.delete(RedisKeys.USER_FAVORITES_PREFIX + userId);
            }
            redisTemplate.opsForSet().add(RedisKeys.USER_FAVORITES_PREFIX + userId, video_id);
        } catch (Exception e) {
            throw new RuntimeException("请勿重复点赞");
        }
        //更新用户token
        updateTokenUtils.updateTokenExpireTimeUtils(userId.toString());
        /**@author zzzi
         * @date 2024/4/2 13:12
         * 异步处理剩下的逻辑，流量削峰
         * 发送两个异步消息，分别更新用户信息和视频信息
         */

        //根据视频id获取到作者id
        VideoDO videoDO = videoMapper.selectById(videoId);
        long[] ids = new long[2];
        ids[0] = userId;//保存点赞人的id
        ids[1] = videoDO.getAuthorId();//保存被点赞人的id
        //发送点赞任何被点赞人的id
        rabbitTemplate.convertAndSend(RabbitMQKeys.FAVORITE_USER, ids);
        //发送视频id
        rabbitTemplate.convertAndSend(RabbitMQKeys.FAVORITE_VIDEO, videoId);
    }

    /**
     * @author zzzi
     * @date 2024/4/2 12:54
     * 用户取消点赞
     * 同步操作
     * 1. 点赞表删除
     * 2. 用户点赞缓存删除
     * 异步操作，两个队列都监听这个异步消息
     * - 用户表更新，A的点赞数-1，B的获赞总数-1
     * - 用户信息缓存更新，A点赞数-1，B获赞总数-1
     * - 视频表更新：B的对应视频点赞数-1
     * - B的视频信息缓存更新，点赞数-1
     */
    @Override
    @Transactional
    public void favoriteUnAction(String token, String video_id) {
        log.info("用户取消点赞service");
        Long userId = JwtUtils.getUserIdByToken(token);
        Long videoId = Long.valueOf(video_id);
        LambdaQueryWrapper<FavoriteDO> queryFavorite = new LambdaQueryWrapper<>();
        //根据用户id和视频id定位到对应的点赞记录
        queryFavorite.eq(FavoriteDO::getUserId, userId).eq(FavoriteDO::getVideoId, videoId);

        //删除用户点赞记录
        int delete = favoriteMapper.delete(queryFavorite);
        if (delete != 1) {
            throw new RuntimeException("取消点赞失败");
        }

        //清空用户点赞缓存列表
        redisTemplate.delete(RedisKeys.USER_FAVORITES_PREFIX + userId);
        //更新用户token缓存
        updateTokenUtils.updateTokenExpireTimeUtils(userId.toString());
        //发送异步消息，处理剩下的业务
        /**@author zzzi
         * @date 2024/4/2 13:32
         * 发送两个异步消息
         * 分别更新用户信息和视频信息
         */
        //根据视频id获取到作者id
        VideoDO videoDO = videoMapper.selectById(videoId);
        long[] ids = new long[2];
        ids[0] = userId;
        ids[1] = videoDO.getAuthorId();
        rabbitTemplate.convertAndSend(RabbitMQKeys.UN_FAVORITE_USER, ids);
        rabbitTemplate.convertAndSend(RabbitMQKeys.UN_FAVORITE_VIDEO, videoId);

    }

    /**
     * @author zzzi
     * @date 2024/4/2 12:54
     * 获取用户点赞列表
     * 先从缓存中获取，缓存中获取不到再从数据库中获取
     */
    @Override
    public List<VideoVO> getFavoriteList(String user_id, String token) {
        log.info("获取用户点赞列表service");

        //判断用户是否登录
        String cacheToken = redisTemplate.opsForValue().get(RedisKeys.USER_TOKEN_PREFIX + user_id);
        if (cacheToken == null || "".equals(cacheToken) || !token.equals(cacheToken))
            throw new VideoListException("当前用户未登录");

        //先从缓存中获取
        Set<String> members = redisTemplate.opsForSet().members(RedisKeys.USER_FAVORITES_PREFIX + user_id);
        if (!members.isEmpty()) {//缓存中获取到了
            return packageFavoriteList(members, user_id);
        } else {//缓存中没有，此时缓存重建
            try {
                //加上互斥锁
                long currentThreadId = Thread.currentThread().getId();
                Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.USER_FAVORITES_PREFIX + user_id + "_mutex", currentThreadId + "");
                //没加上互斥锁
                if (!absent) {
                    Thread.sleep(50);
                    FavoriteService favoriteService = (FavoriteService) AopContext.currentProxy();
                    favoriteService.getFavoriteList(user_id, token);
                }
                //加上互斥锁，二次判断获取缓存中的内容
                members = redisTemplate.opsForSet().members(RedisKeys.USER_FAVORITES_PREFIX + user_id);
                if (!members.isEmpty()) {//缓存中获取到了
                    return packageFavoriteList(members, user_id);
                }
                //到这里缓存中是真的没有，此时缓存重建
                return rebuildUserFavoriteList(user_id);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new VideoListException("获取用户喜欢列表失败");
            } finally {
                //最后需要删除互斥锁
                String currentThreadId = Thread.currentThread().getId() + "";
                String threadId = redisTemplate.opsForValue().get(RedisKeys.USER_FAVORITES_PREFIX + user_id + "_mutex");
                //加锁的就是当前线程才解锁
                if (threadId.equals(currentThreadId)) {
                    redisTemplate.delete(RedisKeys.USER_FAVORITES_PREFIX + user_id + "_mutex");
                }
            }
        }
    }

    /**
     * @author zzzi
     * @date 2024/4/2 15:02
     * 重建用户喜欢列表缓存
     * 并返回喜欢列表
     */
    private List<VideoVO> rebuildUserFavoriteList(String user_id) {
        LambdaQueryWrapper<FavoriteDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FavoriteDO::getUserId, Long.valueOf(user_id)).select(FavoriteDO::getVideoId);
        List<FavoriteDO> favoriteDOList = favoriteMapper.selectList(queryWrapper);

        Set<String> favoriteIdSet = new HashSet<>();
        //数据库中没有
        if (favoriteDOList == null || favoriteDOList.isEmpty()) {
            //缓存中保存默认值，5分钟过期
            redisTemplate.opsForSet().add(RedisKeys.USER_FAVORITES_PREFIX + user_id, RedisDefaultValue.REDIS_DEFAULT_VALUE);
            redisTemplate.expire(RedisKeys.USER_FAVORITES_PREFIX + user_id, 5, TimeUnit.MINUTES);
            //将默认值打包
            favoriteIdSet.add(RedisDefaultValue.REDIS_DEFAULT_VALUE);
        } else {//数据库中有

            //删除可能存在的默认值
            redisTemplate.delete(RedisKeys.USER_FAVORITES_PREFIX + user_id);
            //依次取出用户喜欢视频的视频Id
            for (FavoriteDO favoriteDO : favoriteDOList) {
                String videoId = favoriteDO.getVideoId() + "";
                //保存到id集合和缓存中
                redisTemplate.opsForSet().add(RedisKeys.USER_FAVORITES_PREFIX + user_id, videoId);
                favoriteIdSet.add(videoId);
            }
        }
        return packageFavoriteList(favoriteIdSet, user_id);

    }

    /**
     * @author zzzi
     * @date 2024/4/2 14:43
     * 将用户喜欢列表的数据打包返回
     */
    private List<VideoVO> packageFavoriteList(Set<String> members, String user_id) {
        List<VideoVO> favoriteList = null;
        //缓存中保存的不是默认值，此时才建立喜欢列表
        if (!members.contains(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
            favoriteList = new ArrayList<>();
            //保存所有作者的详细信息，防止多次获取作者信息
            Map<Long, UserVO> userVOMap = new HashMap<>();
            //按照每一个视频的id获取到视频的信息
            for (String videoId : members) {
                VideoDO videoDO = videoService.getVideoInfo(videoId);
                //防止视频id出错没获取到视频
                if (videoDO != null) {
                    //获取当前视频的作者信息
                    Long authorId = videoDO.getAuthorId();
                    UserVO userVO = null;

                    //获取视频作者详细信息
                    if (userVOMap.containsKey(authorId)) {
                        userVO = userVOMap.get(authorId);
                    } else {
                        //远程调用获取到作者的全部信息
                        userVO = userClient.userInfo(authorId).getUser();
                        //保存到map中便于后期复用
                        userVOMap.put(authorId, userVO);
                    }

                    //封装视频和其作者详细信息到一起
                    VideoVO videoVO = videoService.packageFavoriteVideoVO(videoDO, userVO);

                    //将封装好的视频信息保存到喜欢列表中
                    favoriteList.add(videoVO);
                }
            }
        }

        //最后不管怎么样，更新用户token过期时间
        updateTokenUtils.updateTokenExpireTimeUtils(user_id);
        //返回结果
        return favoriteList;
    }
}
