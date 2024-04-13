package com.zzzi.videoservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.constant.RedisDefaultValue;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.VideoException;
import com.zzzi.common.exception.VideoListException;
import com.zzzi.common.feign.UserClient;
import com.zzzi.common.result.UserVO;
import com.zzzi.common.utils.*;
import com.zzzi.videoservice.dto.VideoFeedDTO;
import com.zzzi.videoservice.entity.VideoDO;
import com.zzzi.videoservice.mapper.VideoMapper;
import com.zzzi.common.result.VideoVO;
import com.zzzi.videoservice.service.FavoriteService;
import com.zzzi.videoservice.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VideoServiceImpl extends ServiceImpl<VideoMapper, VideoDO> implements VideoService {

    @Autowired
    private UploadUtils uploadUtils;
    //用来远程调用
    @Autowired
    private UserClient userClient;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private UpdateTokenUtils updateTokenUtils;
    @Autowired
    private FavoriteService favoriteService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private Gson gson;
    @Value("${video_save_path}")
    public String VIDEO_SAVE_PATH;
    @Value("${cover_save_path}")
    public String COVER_SAVE_PATH;

    @Value("${video_feed_max_size}")
    public Long VIDEO_FEED_MAX_SIZE;
    @Value("${user_works_max_size}")
    public Long USER_WORKS_MAX_SIZE;
    @Value("${feed_size}")
    public Long FEED_SIZE;

    /**
     * @author zzzi
     * @date 2024/3/27 15:05
     * 用户投稿视频
     * 由于要插入数据，所以要加上事务管理
     * 并且由于要更新缓存，所以要加上互斥锁
     */
    @Override
    @Transactional
    public void postVideo(MultipartFile data, String token, String title) {
        log.info("用户投稿service");
        //根据用户的token解析得到用户的authorId
        Long authorId = JwtUtils.getUserIdByToken(token);

        //视频上传
        VideoDO videoDO = upload(authorId, data, title);
        //将当前对象转换成json格式
        //将当前数据插入视频表中
        /**@author zzzi
         * @date 2024/3/31 15:48
         * 先更新数据库再更新缓存
         */
        videoMapper.insert(videoDO);

        Long videoId = videoDO.getVideoId();
        String videoDOJson = gson.toJson(videoDO);
        String mutex = MD5Utils.parseStrToMd5L32(videoDOJson);
        RLock lock = redissonClient.getLock(RedisKeys.MUTEX_LOCK_PREFIX + mutex);
        try {
            //当前用户投稿，需要做以下工作

            //1. 拿到互斥锁
            //尝试拿到互斥锁，防止同一时间投稿多个重复作品
            /**@author zzzi
             * @date 2024/3/31 15:37
             * 当前线程加上互斥锁
             */
            boolean absent = lock.tryLock();
            //long currentThreadId = Thread.currentThread().getId();
            //Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.MUTEX_LOCK_PREFIX + mutex, currentThreadId + "", 1, TimeUnit.MINUTES);
            //没拿到互斥锁，说明当前视频已经存在并且正在被操作
            if (!absent) {
                throw new VideoException("视频已经存在");
            }

            //2. 更新推荐视频缓存，主要是插入当前作品的id，因为当前作品新投稿，为最新作品
            //按照时间降序排列
            redisTemplate.opsForZSet().add(RedisKeys.VIDEO_FEED, videoId + "", videoDO.getUpdateTime().getTime());
            //推荐视频缓存中的数据过多，此时删除前100个
            if (redisTemplate.opsForZSet().size(RedisKeys.VIDEO_FEED) > VIDEO_FEED_MAX_SIZE) {
                // redis中zset保存的视频超过5000个了，移除掉前100个视频
                //不够100个就删除一半
                redisTemplate.opsForZSet().removeRange(RedisKeys.VIDEO_FEED, 0, Math.min(VIDEO_FEED_MAX_SIZE >> 1, 100));
            }

            //3. 用户作品列表缓存新增，新增之前需要判断是否有默认值
            /*这一步操作使用binlog监听实现同步双写，不在这里手动实现*/
            /**@author zzzi
             * @date 2024/3/30 14:23
             *用户作品超过指定就从缓存中删除之前投搞的作品
             * 因为用户主页经常访问的也就是前多少个视频
             * 用户作品缓存新增的操作放到binlog监听中实现
             */
            //如果用户作品列表中有默认值，此时先删除默认值再添加
            //List<String> userWorkList = redisTemplate.opsForList().range(RedisKeys.USER_WORKS_PREFIX + authorId, 0, -1);
            //if (userWorkList.contains(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
            //    redisTemplate.delete(RedisKeys.USER_WORKS_PREFIX + authorId);
            //}
            //redisTemplate.opsForList().leftPush(RedisKeys.USER_WORKS_PREFIX + authorId, videoId + "");
            //while (redisTemplate.opsForList().size(RedisKeys.USER_WORKS_PREFIX + authorId) > USER_WORKS_MAX_SIZE) {
            //    //从右边删除，代表删除最早投稿的视频
            //    redisTemplate.opsForList().rightPop(RedisKeys.USER_WORKS_PREFIX + authorId);
            //}
            //4. 视频信息缓存新增
            redisTemplate.opsForValue().set(RedisKeys.VIDEO_INFO_PREFIX + videoId, videoDOJson);
            /**@author zzzi
             * @date 2024/3/27 19:29
             * 直接让用户作品数+1即可
             * 并且更新用户的基本信息
             */
            //5. 异步更新用户表中的作品数和缓存中的用户信息

            // 5.1.全局唯一的消息ID，需要封装到CorrelationData中
            CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
            // 5.2.添加callback
            //根据返回的是ack还是nack判断消息是否到达交换机
            correlationData.getFuture().addCallback(
                    //消息发送成功（不一定到交换机），此时执行下面的判断逻辑
                    result -> {
                        if (result.isAck()) {
                            // 3.1.ack，消息成功
                            log.debug("消息发送成功, ID:{}", correlationData.getId());
                        } else {//这里可以抛出一个异常，相应的事务就会回滚
                            // 3.2.nack，消息失败
                            log.error("消息发送失败, ID:{}, 原因{}", correlationData.getId(), result.getReason());
                            throw new VideoException("视频保存失败");
                        }
                    },
                    //消息发送失败执行的逻辑
                    ex -> log.error("消息发送异常, ID:{}, 原因{}", correlationData.getId(), ex.getMessage())
            );
            rabbitTemplate.convertAndSend(RabbitMQKeys.POST_VIDEO_EXCHANGE, RabbitMQKeys.VIDEO_POST, authorId, correlationData);

            //到这里就可以返回
            log.info("投稿成功");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new VideoException("视频保存失败");
        } finally {//释放互斥锁
            /**@author zzzi
             * @date 2024/3/31 15:37
             * 需要是加锁的线程才能解锁
             */
            lock.unlock();
            //String currentThreadId = Thread.currentThread().getId() + "";
            //String threadId = redisTemplate.opsForValue().get(RedisKeys.MUTEX_LOCK_PREFIX + mutex);
            ////加锁的就是当前线程才解锁
            //if (currentThreadId.equals(threadId)) {
            //    redisTemplate.delete(RedisKeys.MUTEX_LOCK_PREFIX + mutex);
            //}
        }
    }


    /**
     * @author zzzi
     * @date 2024/3/28 14:40
     * 在这里获取当前用户的所有作品
     * 使用默认值解决了缓存穿透问题
     * 使用双重检查锁解决缓存多次重建问题
     */
    @Override
    public List<VideoVO> getPublishListByAuthorId(String token, Long user_id) {
        log.warn("获取用户列表service，传递的token为：{}", token);
        //判断用户是否登录
        //不仅仅是自己获取自己的作品，还可能有别人看。所以根据业务打开
        //String cacheToken = redisTemplate.opsForValue().get(RedisKeys.USER_TOKEN_PREFIX + user_id);
        //if (cacheToken == null || "".equals(cacheToken) || !token.equals(cacheToken))
        //    throw new VideoListException("当前用户未登录");

        /**@author zzzi
         * @date 2024/4/1 15:11
         * OpenFeign远程调用，得到作者的详细信息
         */
        UserVO userVO = userClient.userInfo(user_id).getUser();

        //根据当前用户的user_id获取到当前用户缓存的所有作品的id
        List<String> userWorkList = redisTemplate.opsForList().range(RedisKeys.USER_WORKS_PREFIX + user_id, 0, -1);

        //分为两种情况：
        //1. 缓存中有:
        //      1.1 防止缓存穿透的默认值
        //      1.2 是真的有
        //2. 缓存中没有：此时缓存重建，重建之前需要拿到互斥锁并进行双重检查，防止缓存多次重建
        //缓存重建需要注意的是数据库中没有数据时，此时缓存中要保存设置有效期的默认值
        if (userWorkList != null && !userWorkList.isEmpty()) {//缓存中有
            return packageVideoListVO(userWorkList, userVO, user_id.toString(), token);
        } else {//缓存中没有
            RLock lock = redissonClient.getLock(RedisKeys.USER_WORKS_PREFIX + user_id + "_mutex");
            try {
                //1. 先尝试获取互斥锁，没获取到一直尝试，互斥锁的key为用户作品列表的key
                boolean absent = lock.tryLock();
                //long currentThreadId = Thread.currentThread().getId();
                //Boolean absent = redisTemplate.opsForValue().
                //        setIfAbsent(RedisKeys.USER_WORKS_PREFIX + user_id + "_mutex", currentThreadId + "", 1, TimeUnit.MINUTES);
                //没获取到互斥锁，说明当前用户的作品列表正在被被人操作，此时重试
                if (!absent) {
                    Thread.sleep(50);
                    //不停地调用自己，但是不使用循环依赖的方式
                    VideoService videoService = (VideoService) AopContext.currentProxy();
                    return videoService.getPublishListByAuthorId(token, user_id);
                }
                //2. 获取到互斥锁进行二次判断，防止缓存重建多次
                userWorkList = redisTemplate.opsForList().range(RedisKeys.USER_WORKS_PREFIX + user_id, 0, -1);
                if (!userWorkList.isEmpty()) {//缓存中有
                    return packageVideoListVO(userWorkList, userVO, user_id.toString(), token);
                }
                //3. 到这里才真正进行缓存重建
                return rebuildUserWorkListCache(user_id, userVO, token);
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new VideoListException("获取用户作品列表失败");
            } finally {//最后释放互斥锁
                /**@author zzzi
                 * @date 2024/3/31 15:37
                 * 需要是加锁的线程才能解锁
                 */
                lock.unlock();
                //String currentThreadId = Thread.currentThread().getId() + "";
                //String threadId = redisTemplate.opsForValue().get(RedisKeys.USER_WORKS_PREFIX + user_id + "_mutex");
                ////加锁的就是当前线程才解锁
                //if (currentThreadId.equals(threadId)) {
                //    redisTemplate.delete(RedisKeys.USER_WORKS_PREFIX + user_id + "_mutex");
                //}
            }
        }
    }

    /**
     * @author zzzi
     * @date 2024/3/29 12:23
     * 如果当前用户登陆了，也就是传递了token，需要判断关注状态和点赞状态
     * 当前用户没有登陆，默认没关注
     * 缓存中没有就是别的操作将这个缓存删除了，此时
     * 拼接VideoVO，is_favorite和is_follow有用户登录才需要判断
     */
    @Override
    public VideoFeedDTO getFeedList(Long latest_time, String token) {
        log.warn("获取推荐视频service，token为：{}", token);
        //从缓存中获取
        Set<String> videoFeedSet = redisTemplate.opsForZSet().reverseRangeByScore(RedisKeys.VIDEO_FEED, 0, latest_time - 1, 0, 30);
        List<VideoDO> videoDOList = new ArrayList<>();
        List<VideoVO> feedList = null;
        //获取到的推荐视频不足，此时从数据库中获取
        if (videoFeedSet.size() < FEED_SIZE) {
            //从数据库中获取前FEED_SIZE个
            Page<VideoDO> page = new Page<>(1, FEED_SIZE);
            page.addOrder(OrderItem.desc("update_time"));
            LambdaQueryWrapper<VideoDO> queryWrapper = new LambdaQueryWrapper<>();
            //默认查询小于当前推荐时间的30个视频，应该有三个
            queryWrapper.ge(VideoDO::getUpdateTime, latest_time);

            /**@author zzzi
             * @date 2024/4/2 17:08
             * 按照更新时间降序排列，查询一页数据，每一页默认有30条数据
             */
            videoDOList = videoMapper.selectPage(page, queryWrapper).getRecords();

            //todo：缓存重建，还有一点小问题，视频更新之后，按照更新时间放入Zset中，此时Zset中有两个相同的视频id
            //todo：好像Zset中的视频id不能重复，但是分数可以重复，所以这里应该没有问题
            rebuildFeedVideoList(videoDOList);
        } else {//缓存中够,此时根据视频id获取视频的实体类
            //先将数缓存中的视频id转换成VideoDO
            for (String videoId : videoFeedSet) {
                VideoDO videoDO = getVideoInfo(videoId);
                videoDOList.add(videoDO);
            }
        }
        //到这里反正videoDOList已经形成了，也就是得到了推荐视频的VideoDO
        /**@author zzzi
         * @date 2024/4/2 18:22
         * 得到下一次推荐视频的时间
         */
        if (videoDOList != null) {
            long next_time = videoDOList.get(videoDOList.size() - 1).getUpdateTime().getTime();
            //传递token 是因为要判断是否需要设置is_favorite和is_follow
            if (token != null) {
                feedList = packageFeedVideoListWithToken(videoDOList, token);
            } else {
                feedList = packageFeedVideoListWithOutToken(videoDOList);
            }
            //返回最终的结果
            VideoFeedDTO videoFeedDTO = new VideoFeedDTO();
            videoFeedDTO.setFeed_list(feedList);
            videoFeedDTO.setNext_time(next_time);
            return videoFeedDTO;
        }
        //数据库和缓存中都没有视频，此时返回null
        return null;
    }

    /**
     * @author zzzi
     * @date 2024/4/2 18:27
     * 重建推荐视频的缓存，如果传递的视频列表中有数据的话
     * zset中的值不能重复，所以不会出现重复放入的情况
     */
    private void rebuildFeedVideoList(List<VideoDO> videoDOList) {
        if (videoDOList != null) {
            //将新查询到的数据插入缓存中
            for (VideoDO videoDO : videoDOList) {
                Long videoId = videoDO.getVideoId();
                redisTemplate.opsForZSet().add(RedisKeys.VIDEO_FEED, videoId + "", videoDO.getUpdateTime().getTime());
            }
        }
    }

    /**
     * @author zzzi
     * @date 2024/4/2 17:15
     * 打包形成推荐视频的列表，这个方法没有用token，直接返回
     */
    private List<VideoVO> packageFeedVideoListWithOutToken(List<VideoDO> videoDOList) {
        List<VideoVO> feedList = new ArrayList<>();
        //没有token，这里的用户一定是没关注的
        Map<Long, UserVO> userVOMap = new HashMap<>();
        for (VideoDO videoDO : videoDOList) {
            Long authorId = videoDO.getAuthorId();
            UserVO authorVO = null;
            if (userVOMap.containsKey(authorId)) {
                authorVO = userVOMap.get(authorId);
            } else {
                //远程调用获取用户的详细信息
                authorVO = userClient.userInfo(authorId).getUser();
                //保存到map中便于后面复用
                userVOMap.put(authorId, authorVO);
            }

            //没有token，所以就在这里打包即可
            VideoVO videoVO = new VideoVO();
            videoVO.setId(videoDO.getVideoId());

            videoVO.setAuthor(authorVO);
            videoVO.setPlay_url(videoDO.getPlayUrl());
            videoVO.setCover_url(videoDO.getCoverUrl());
            videoVO.setFavorite_count(videoDO.getFavoriteCount());
            videoVO.setComment_count(videoDO.getCommentCount());
            videoVO.setTitle(videoDO.getTitle());

            //打包好的视频直接返回
            feedList.add(videoVO);
        }
        return feedList;
    }

    /**
     * @author zzzi
     * @date 2024/4/2 17:15
     * 打包形成推荐视频的列表，这个方法利用token设置is_favorite和is_follow
     */
    private List<VideoVO> packageFeedVideoListWithToken(List<VideoDO> videoDOList, String token) {
        List<VideoVO> feedList = new ArrayList<>();
        Map<Long, UserVO> userVOMap = new HashMap<>();
        //解析得到当前用户的用户id
        Long userId = JwtUtils.getUserIdByToken(token);
        //获取当前用户的关注列表
        List<UserVO> followList = userClient.getFollowList(userId.toString(), token).getUser_list();

        //针对每个视频，如果视频作者在当前用户的关注列表中，那么is_follow为true
        for (VideoDO videoDO : videoDOList) {
            Long authorId = videoDO.getAuthorId();
            UserVO userVO = null;
            if (userVOMap.containsKey(authorId)) {//有直接复用
                userVO = userVOMap.get(authorId);
            } else {
                userVO = userClient.userInfo(authorId).getUser();
                //当前用户的关注列表中有这个作者
                if (followList != null && followList.contains(userVO)) {
                    userVO.setIs_follow(true);
                }
                //使用Map保存当前与作者的关注关系，后期直接复用即可
                userVOMap.put(authorId, userVO);
            }

            //有了作者和视频信息，将其打包成一个VideoVO
            //这里面会判断当前用户是否对当前视频点赞了
            VideoVO videoVO = packageVideoVO(videoDO, userVO, userId.toString(), token);
            feedList.add(videoVO);
        }
        return feedList;
    }

    /**
     * @author zzzi
     * @date 2024/3/28 16:16
     * 重建用户作品缓存，然后将查询到的数据返回
     * 用户作品缓存中保存的是作品id
     */
    public List<VideoVO> rebuildUserWorkListCache(Long user_id, UserVO userVO, String token) {
        log.info("重建用户作品列表缓存service");
        //从数据库中查询到当前用户的所有数据，有两种情况：
        //1. 查询到了数据，此时正常重建缓存
        //2. 没查询到数据，为了防止缓存穿透，此时存储默认值
        LambdaQueryWrapper<VideoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoDO::getAuthorId, user_id);
        //获取当前作者的全部作品
        List<VideoDO> videoDOList = videoMapper.selectList(queryWrapper);

        List<String> userWorkList = new ArrayList<>();
        if (videoDOList == null || videoDOList.size() == 0) {//没有作品，存储默认值
            //将数据缓存到用户作品缓存中
            userWorkList.add(RedisDefaultValue.REDIS_DEFAULT_VALUE);
            /**@author zzzi
             * @date 2024/3/29 13:36
             * 缓存中存储5分钟过期的默认值，防止缓存穿透
             */
            redisTemplate.opsForList().leftPush(RedisKeys.USER_WORKS_PREFIX + user_id, RedisDefaultValue.REDIS_DEFAULT_VALUE);
            redisTemplate.expire(RedisKeys.USER_WORKS_PREFIX + user_id, 5, TimeUnit.MINUTES);
        } else {//查询到了数据，存储真实值
            for (VideoDO videoDO : videoDOList) {
                Long videoId = videoDO.getVideoId();
                userWorkList.add(videoId + "");
            }

            //将数据缓存到用户作品缓存中
            //先删除缓存中的默认值（如果没过期，存在的话）
            redisTemplate.delete(RedisKeys.USER_WORKS_PREFIX + user_id);
            //正常的作品不设置有效期
            /**@author zzzi
             * @date 2024/3/30 14:23
             * 用户作品超过大小就删除一点
             * 只缓存前多少个视频，因为主页经常访问的也就是前多少个视频
             */
            redisTemplate.opsForList().leftPushAll(RedisKeys.USER_WORKS_PREFIX + user_id, userWorkList);
            while (redisTemplate.opsForList().size(RedisKeys.USER_WORKS_PREFIX + user_id) > USER_WORKS_MAX_SIZE) {
                //从右边删除，代表删除最早投稿的视频
                redisTemplate.opsForList().rightPop(RedisKeys.USER_WORKS_PREFIX + user_id);
            }
        }
        //最后将数据打包返回
        return packageVideoListVO(userWorkList, userVO, user_id.toString(), token);
    }

    /**
     * @author zzzi
     * @date 2024/3/28 15:46
     * 将从缓存中获取到的List<String> userWorkList
     */
    public List<VideoVO> packageVideoListVO(List<String> userWorkList, UserVO userVO, String user_id, String token) {
        log.info("打包用户作品列表service");
        /**@author zzzi
         * @date 2024/3/28 15:57
         * 防止缓存穿透，此时直接返回null,缓存中已经存储了默认值
         */
        if (userWorkList.contains(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
            return null;
        }
        //到这里就是缓存中真的有，此时直接打包返回
        List<VideoVO> videoVOList = new ArrayList<>(userWorkList.size());
        //将从数据库中查询到的数据打包成json缓存到数据库中
        for (String videoId : userWorkList) {
            //根据videoId得到video的详细信息
            VideoDO videoDO = getVideoInfo(videoId);
            //防止视频id出错，获取不到视频
            if (videoDO != null) {
                //将每一个videoDO转换成videoVO，也就是封装视频和其作者的详细信息
                /**@author zzzi
                 * @date 2024/4/2 17:36
                 * 需要判断当前视频的点赞状态
                 */
                VideoVO videoVO = packageVideoVO(videoDO, userVO, user_id, token);
                videoVOList.add(videoVO);
            }

        }
        /**@author zzzi
         * @date 2024/4/2 15:38
         * 不管怎么样，需要更新用户的token
         */
        Long userId = userVO.getId();
        updateTokenUtils.updateTokenExpireTimeUtils(userId.toString());
        return videoVOList;
    }


    /**
     * @author zzzi
     * @date 2024/4/1 16:18
     * 根据视频的id获取视频的详细信息
     * 返回的时videoDO，因为videoVO还需要封装作者信息
     * 所以哪里需要就在哪里封装，不在这里封装，防止传输的数据过大
     */
    @Override
    public VideoDO getVideoInfo(String videoId) {
        VideoDO videoDO = null;
        //1. 先从缓存中获取
        String videoDOJson = redisTemplate.opsForValue().get(RedisKeys.VIDEO_INFO_PREFIX + videoId);
        //不为空也不是默认值
        if (videoDOJson != null && !RedisDefaultValue.REDIS_DEFAULT_VALUE.equals(videoDOJson)) {
            videoDO = gson.fromJson(videoDOJson, VideoDO.class);
        } else {//缓存中没有。尝试缓存重建
            RLock lock = redissonClient.getLock(RedisKeys.VIDEO_INFO_PREFIX + videoId + "_mutex");
            try {
                //使用Redisson获取互斥锁
                boolean absent = lock.tryLock();
                //long currentThreadId = Thread.currentThread().getId();
                //Boolean absent = redisTemplate.opsForValue().
                //        setIfAbsent(RedisKeys.VIDEO_INFO_PREFIX + videoId + "_mutex", currentThreadId + "", 1, TimeUnit.MINUTES);
                //获取互斥锁失败
                if (!absent) {
                    Thread.sleep(50);
                    VideoService videoService = (VideoService) AopContext.currentProxy();
                    return videoService.getVideoInfo(videoId);
                }
                //再次尝试从缓存中获取
                videoDOJson = redisTemplate.opsForValue().get(RedisKeys.VIDEO_INFO_PREFIX + videoId);
                //不为空也不是默认值
                if (videoDOJson != null && !RedisDefaultValue.REDIS_DEFAULT_VALUE.equals(videoDOJson)) {
                    videoDO = gson.fromJson(videoDOJson, VideoDO.class);
                } else {
                    //到这里需要缓存重建
                    videoDO = rebuildVideoInfoCache(videoId);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                //删除互斥锁
                lock.unlock();
                //String currentThreadId = Thread.currentThread().getId() + "";
                //String threadId = redisTemplate.opsForValue().get(RedisKeys.VIDEO_INFO_PREFIX + videoId + "_mutex");
                ////加锁和解锁进程是一个才删除
                //if (currentThreadId.equals(threadId)) {
                //    redisTemplate.delete(RedisKeys.VIDEO_INFO_PREFIX + videoId + "_mutex");
                //}
            }
        }
        return videoDO;
    }

    /**
     * @author zzzi
     * @date 2024/4/1 16:24
     * 重建视频信息缓存
     */
    private VideoDO rebuildVideoInfoCache(String videoId) {
        LambdaQueryWrapper<VideoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoDO::getVideoId, Long.valueOf(videoId));

        VideoDO videoDO = videoMapper.selectOne(queryWrapper);
        //数据库中没有
        if (videoDO == null) {
            /**@author zzzi
             * @date 2024/4/1 16:26
             * 缓存重建为5分钟过期的默认值
             */
            redisTemplate.opsForValue().set(RedisKeys.VIDEO_INFO_PREFIX + videoId, RedisDefaultValue.REDIS_DEFAULT_VALUE, 5, TimeUnit.MINUTES);
        } else {//数据库中查到了，此时直接更新缓存
            String videoDOJson = gson.toJson(videoDO);
            //删除可能没过期的默认值
            redisTemplate.delete(RedisKeys.VIDEO_INFO_PREFIX + videoId);
            redisTemplate.opsForValue().set(RedisKeys.VIDEO_INFO_PREFIX + videoId, videoDOJson);
        }
        return videoDO;
    }

    /**
     * @author zzzi
     * @date 2024/3/29 13:43
     * 这个函数在获取用户作品列表和推荐视频列表时都会使用
     * 1. 获取用户作品列表时，用户的关注状态默认为true
     * 2. 获取推荐视频列表时，用户的关注状态需要手动判断
     */
    @Override
    public VideoVO packageVideoVO(VideoDO videoDO, UserVO userVO, String user_id, String token) {
        log.info("打包单个视频service");
        VideoVO videoVO = new VideoVO();
        videoVO.setId(videoDO.getVideoId());
        videoVO.setAuthor(userVO);
        videoVO.setPlay_url(videoDO.getPlayUrl());
        videoVO.setCover_url(videoDO.getCoverUrl());
        videoVO.setFavorite_count(videoDO.getFavoriteCount());
        videoVO.setComment_count(videoDO.getCommentCount());
        videoVO.setTitle(videoDO.getTitle());
        /**@author zzzi
         * @date 2024/3/28 15:54
         * 获取当前用户的点赞列表，如果点赞列表中包含这个视频，那么就应该设置is_favorite为true
         */
        List<VideoVO> favoriteList = favoriteService.getFavoriteList(user_id, token);
        //这里可以用contains，因为VideoVO已经重写了equals和hashCode
        if (favoriteList != null && favoriteList.contains(videoVO)) {
            videoVO.setIs_favorite(true);
        }
        return videoVO;
    }

    @Override
    public VideoVO packageFavoriteVideoVO(VideoDO videoDO, UserVO userVO) {
        log.info("打包点赞单个视频service");
        VideoVO videoVO = new VideoVO();
        videoVO.setId(videoDO.getVideoId());
        videoVO.setAuthor(userVO);
        videoVO.setPlay_url(videoDO.getPlayUrl());
        videoVO.setCover_url(videoDO.getCoverUrl());
        videoVO.setFavorite_count(videoDO.getFavoriteCount());
        videoVO.setComment_count(videoDO.getCommentCount());
        videoVO.setTitle(videoDO.getTitle());
        //这个方法中，视频一定是被点赞的，因为这里打包的是点赞视频
        videoVO.setIs_favorite(true);
        return videoVO;
    }


    /**
     * @author zzzi
     * @date 2024/3/27 16:13
     * 封装一个视频上传的方法，将视频和封面保存到本地和云端
     */
    public VideoDO upload(Long authorId, MultipartFile data, String title) {
        log.info("视频上传获得地址service");
        try {
            String videoName = authorId + "_" + UUID.randomUUID() + "_video" + ".mp4";
            String coverName = authorId + "_" + UUID.randomUUID() + "_cover" + ".jpg";
            //MultipartFile转File
            File video_dir = new File(VIDEO_SAVE_PATH);
            if (!video_dir.exists()) {
                video_dir.mkdirs();
            }
            File video = new File(video_dir, videoName);
            data.transferTo(video);

            //抓取一帧存到指定的文件夹中并返回抓取到的文件
            File cover = VideoUtils.fetchPic(video, COVER_SAVE_PATH + coverName);

            //上传文件
            String coverUrl = uploadUtils.upload(cover, "_cover.jpg");
            String videoUrl = uploadUtils.upload(video, "_video.mp4");
            /**@author zzzi
             * @date 2024/3/24 10:05
             * 拿到本地和云端地址，数据库想保存哪个就保存哪个
             * 做到数据双备份
             */
            log.info("封面上传地址为:{}", coverUrl);
            log.info("视频上传地址为:{}", videoUrl);

            log.info("封面本地地址为：{}", COVER_SAVE_PATH + coverName);
            log.info("视频本地地址为：{}", VIDEO_SAVE_PATH + videoName);
            VideoDO videoDO = new VideoDO();
            videoDO.setAuthorId(authorId);
            //videoDO.setCoverUrl(COVER_SAVE_PATH + coverName);
            //videoDO.setPlayUrl(VIDEO_SAVE_PATH + videoName);
            //视频和封面先写死，后期再修改，这样运行得快
            //videoDO.setPlayUrl("https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok/video/2024-03-24T10%3A12%3A46.366e045b588-7292-4d86-924e-af74e62da0e8_video.mp4");
            //videoDO.setCoverUrl("https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok/cover/2024-03-24T10%3A12%3A44.195978803f4-f52b-45ef-bf30-79c4c7e6ead2_cover.jpg");
            videoDO.setCoverUrl(coverUrl);
            videoDO.setPlayUrl(videoUrl);
            videoDO.setTitle(title);

            return videoDO;
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new VideoException("视频上传失败");
        }
    }
}
