package com.zzzi.videoservice.controller;

import com.zzzi.common.utils.UploadUtils;
import com.zzzi.common.utils.VideoUtils;
import com.zzzi.videoservice.entity.VideoDO;
import com.zzzi.videoservice.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/douyin")
@Slf4j
public class VideoController {

    @Autowired
    private UploadUtils uploadUtils;
    @Autowired
    private VideoService videoService;
    @Value("${video_save_path}")
    public String VIDEO_SAVE_PATH;
    @Value(("${cover_save_path}"))
    public String COVER_SAVE_PATH;

    @GetMapping("/publish/list")
    public void getPublishList(String token, String user_id) {

    }

    @GetMapping("/feed")
    public void getFeedList(String latest_time, String token) {

    }

    @PostMapping("/publish/action")
    public void postVideo(MultipartFile data, String token, String title) {
        try {
            //每次都给视频生成新的文件名，这里后期可改造为用户id,防止重复
            String prefixName = token + UUID.randomUUID();
            String videoName = prefixName + "_video" + ".mp4";
            String coverName = prefixName + "_cover" + ".jpg";
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
            videoDO.setAuthorId(1726534176241L);
            videoDO.setCoverUrl(coverUrl);
            videoDO.setPlayUrl(videoUrl);

            //调用videoService将对象存储到数据库中
            videoService.save(videoDO);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }
}
