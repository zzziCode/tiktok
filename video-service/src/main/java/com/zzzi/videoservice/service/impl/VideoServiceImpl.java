package com.zzzi.videoservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.constant.RedisDefaultValue;
import com.zzzi.common.constant.RedisKeys;
import com.zzzi.common.exception.UserException;
import com.zzzi.common.exception.VideoException;
import com.zzzi.common.feign.UserClient;
import com.zzzi.common.result.UserVO;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.common.utils.MD5Utils;
import com.zzzi.common.utils.UploadUtils;
import com.zzzi.common.utils.VideoUtils;
import com.zzzi.videoservice.controller.VideoController;
import com.zzzi.videoservice.entity.VideoDO;
import com.zzzi.videoservice.mapper.VideoMapper;
import com.zzzi.videoservice.result.VideoListVO;
import com.zzzi.videoservice.result.VideoVO;
import com.zzzi.videoservice.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sun.awt.Mutex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    @Value("${video_save_path}")
    public String VIDEO_SAVE_PATH;
    @Value("${cover_save_path}")
    public String COVER_SAVE_PATH;

    @Value("${video_feed_max_size}")
    public Long VIDEO_FEED_MAX_SIZE;


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
        //根据用户的token解析得到用户的authorId
        Long authorId = JwtUtils.getUserIdByToken(token);

        //视频上传
        VideoDO videoDO = upload(authorId, data, title);
        //将当前对象转换成json格式
        //将当前数据插入视频表中
        videoMapper.insert(videoDO);

        Gson gson = new Gson();
        String videoDOJson = gson.toJson(videoDO);
        String mutex = MD5Utils.parseStrToMd5L32(videoDOJson);
        try {
            //当前用户投稿，需要做以下工作

            //1. 拿到互斥锁
            //尝试拿到互斥锁，防止同一时间投稿多个重复作品
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.MUTEX_LOCK_PREFIX + mutex, "");
            //没拿到互斥锁，说明当前视频已经存在并且正在被操作
            if (!absent) {
                throw new VideoException("视频已经存在");
            }

            //2. 更新推荐视频缓存，主要是插入当前作品，因为当前作品新投稿，为最新作品
            //按照时间降序排列
            redisTemplate.opsForZSet().add(RedisKeys.VIDEO_FEED,
                    videoDOJson, videoDO.getCreateTime().getTime());
            //推荐视频缓存中的数据过多，此时删除前100个
            if (redisTemplate.opsForZSet().size(RedisKeys.VIDEO_FEED) > VIDEO_FEED_MAX_SIZE) {
                // redis中zset保存的视频超过5000个了，移除掉前100个视频
                //不够100个就删除一半
                redisTemplate.opsForZSet().removeRange(RedisKeys.VIDEO_FEED, 0, Math.min(VIDEO_FEED_MAX_SIZE >> 1, 100));
            }

            //3. 用户作品列表缓存新增
            redisTemplate.opsForList().leftPush(RedisKeys.USER_WORKS_PREFIX + authorId, videoDOJson);

            /**@author zzzi
             * @date 2024/3/27 19:29
             * 直接让用户作品数+1即可
             * 并且更新用户的基本信息
             */
            //4. 异步更新用户表中的作品数和缓存中的用户信息
            rabbitTemplate.convertAndSend(RabbitMQKeys.EXCHANGE_NAME, RabbitMQKeys.VIDEO_POST, authorId);

            //到这里就可以返回
            log.info("投稿成功");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new VideoException("视频保存失败");
        } finally {//释放互斥锁
            redisTemplate.delete(RedisKeys.MUTEX_LOCK_PREFIX + mutex);
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
        //判断用户是否登录
        String cacheToken = redisTemplate.opsForValue().get(RedisKeys.USER_TOKEN_PREFIX + user_id);
        if (cacheToken == null || "".equals(cacheToken) || !token.equals(cacheToken))
            throw new UserException("当前用户未登录");

        //发起远程调用获取当前用户的详细信息
        UserVO userVO = userClient.userInfo(user_id).getUser();

        //根据当前用户的user_id获取到当前用户缓存的所有作品信息
        List<String> userWorkList = redisTemplate.opsForList().range(RedisKeys.USER_WORKS_PREFIX + user_id, 0, -1);

        //分为两种情况：
        //1. 缓存中有:
        //      1.1 防止缓存穿透的默认值
        //      1.2 是真的有
        //2. 缓存中没有：此时缓存重建，重建之前需要拿到互斥锁并进行双重检查，防止缓存多次重建
        //缓存重建需要注意的是数据库中没有数据时，此时缓存中要保存设置有效期的默认值
        if (userWorkList.size() > 0) {//缓存中有
            return packageVideoListVO(userWorkList, userVO);
        } else {//缓存中没有
            try {
                //1. 先尝试获取互斥锁，没获取到一直尝试，互斥锁的key为用户作品列表的key
                Boolean absent = redisTemplate.opsForValue().setIfAbsent(RedisKeys.USER_WORKS_PREFIX + user_id, "");
                //没获取到互斥锁，说明当前用户的作品列表正在被被人操作，此时重试
                if (!absent) {
                    Thread.sleep(50);
                    //不停地调用自己
                    getPublishListByAuthorId(token, user_id);
                }
                //2. 获取到互斥锁进行二次判断，防止缓存重建多次
                userWorkList = redisTemplate.opsForList().range(RedisKeys.USER_WORKS_PREFIX + user_id, 0, -1);
                if (userWorkList.size() > 0) {//缓存中有
                    return packageVideoListVO(userWorkList, userVO);
                }
                //3. 到这里才真正进行缓存重建
                return rebuildCache(user_id, userVO);
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new VideoException("获取用户作品列表失败");
            } finally {//最后释放互斥锁
                redisTemplate.delete(RedisKeys.USER_WORKS_PREFIX + user_id);
            }
        }
    }

    /**
     * @author zzzi
     * @date 2024/3/28 16:16
     * 缓存重建，然后将查询到的数据返回
     */
    private List<VideoVO> rebuildCache(Long user_id, UserVO userVO) {
        //从数据库中查询到当前用户的所有数据，有两种情况：
        //1. 查询到了数据，此时正常重建缓存
        //2. 没查询到数据，为了防止缓存穿透，此时存储默认值
        LambdaQueryWrapper<VideoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoDO::getAuthorId, user_id);
        //获取当前作者的全部作品
        /**@author zzzi
         * @date 2024/3/28 18:16
         * 这里为什么没查询到视频的id
         */
        List<VideoDO> videoDOList = videoMapper.selectList(queryWrapper);

        List<String> userWorkList = new ArrayList<>();
        if (videoDOList.size() == 0) {//没有作品，存储默认值
            userWorkList.add(RedisDefaultValue.REDIS_DEFAULT_VALUE);
        } else {//查询到了数据，存储真实值
            Gson gson = new Gson();
            for (VideoDO videoDO : videoDOList) {
                String videoDOJson = gson.toJson(videoDO);
                userWorkList.add(videoDOJson);
            }
        }
        //将数据缓存到用户作品缓存中
        redisTemplate.opsForList().leftPushAll(RedisKeys.USER_WORKS_PREFIX + user_id, userWorkList);
        //然后将数据打包返回
        return packageVideoListVO(userWorkList, userVO);
    }

    /**
     * @author zzzi
     * @date 2024/3/28 15:46
     * 将每一个json风格的videoDO转换成videoVO
     */
    private List<VideoVO> packageVideoListVO(List<String> userWorkList, UserVO userVO) {
        /**@author zzzi
         * @date 2024/3/28 15:57
         * 防止缓存穿透，此时直接返回null
         */
        if (userWorkList.get(0).equals(RedisDefaultValue.REDIS_DEFAULT_VALUE)) {
            return null;
        }
        //到这里就是缓存中真的有，此时直接打包返回
        Gson gson = new Gson();
        List<VideoVO> videoVOList = new ArrayList<>(userWorkList.size());
        for (String videoDOJson : userWorkList) {
            VideoDO videoDO = gson.fromJson(videoDOJson, VideoDO.class);
            //将每一个videoDO转换成videoVO
            VideoVO videoVO = packageVideoVO(videoDO, userVO);
            videoVOList.add(videoVO);
        }
        return videoVOList;
    }

    private VideoVO packageVideoVO(VideoDO videoDO, UserVO userVO) {
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
         * todo：后期修改用户自己作品的点赞状态
         */
        videoVO.setIs_favorite(true);
        return videoVO;
    }


    /**
     * @author zzzi
     * @date 2024/3/27 16:13
     * 封装一个视频上传的方法，将视频和封面保存到本地和云端
     */
    private VideoDO upload(Long authorId, MultipartFile data, String title) {
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
            //String coverUrl = uploadUtils.upload(cover, "_cover.jpg");
            //String videoUrl = uploadUtils.upload(video, "_video.mp4");
            /**@author zzzi
             * @date 2024/3/24 10:05
             * 拿到本地和云端地址，数据库想保存哪个就保存哪个
             * 做到数据双备份
             */
            //log.info("封面上传地址为:{}", coverUrl);
            //log.info("视频上传地址为:{}", videoUrl);

            log.info("封面本地地址为：{}", COVER_SAVE_PATH + coverName);
            log.info("视频本地地址为：{}", VIDEO_SAVE_PATH + videoName);
            VideoDO videoDO = new VideoDO();
            videoDO.setAuthorId(authorId);
            videoDO.setCoverUrl(COVER_SAVE_PATH + coverName);
            videoDO.setPlayUrl(VIDEO_SAVE_PATH + videoName);
            videoDO.setTitle(title);

            return videoDO;
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new VideoException("视频上传失败");
        }
    }
}
