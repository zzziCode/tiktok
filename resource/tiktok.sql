DROP TABLE IF EXISTS `users_1`;
CREATE TABLE `users_1` (
  `user_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `username` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL UNIQUE COMMENT '用户姓名',
  `email` varchar(32) NOT NULL,
  `password` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'MD5加密后的密码',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间，自动填充',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间，自动更新',
  `follow_count` int DEFAULT 0 COMMENT '关注总数',
  `follower_count` int DEFAULT 0 COMMENT '粉丝总数',
  `avatar` varchar(200)  DEFAULT 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok_avatar.jpg' COMMENT '用户头像',
  `background_image` varchar(200) DEFAULT 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/background_image.jpg' COMMENT '主页背景',
  `signature` varchar(100) DEFAULT '谢谢你的关注' COMMENT '个人简介',
  `total_favorited` bigint DEFAULT 0 COMMENT '获赞总数',
  `work_count` int DEFAULT 0 COMMENT '作品数',
  `favorite_count` int DEFAULT 0 COMMENT '点赞数',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `unionidx_email_password` (`email`,`password`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `users_2`;
CREATE TABLE `users_2` (
  `user_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `username` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL UNIQUE COMMENT '用户姓名',
  `email` varchar(32) NOT NULL,
  `password` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'MD5加密后的密码',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间，自动填充',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间，自动更新',
  `follow_count` int DEFAULT 0 COMMENT '关注总数',
  `follower_count` int DEFAULT 0 COMMENT '粉丝总数',
  `avatar` varchar(200)  DEFAULT 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok_avatar.jpg' COMMENT '用户头像',
  `background_image` varchar(200) DEFAULT 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/background_image.jpg' COMMENT '主页背景',
  `signature` varchar(100) DEFAULT '谢谢你的关注' COMMENT '个人简介',
  `total_favorited` bigint DEFAULT 0 COMMENT '获赞总数',
  `work_count` int DEFAULT 0 COMMENT '作品数',
  `favorite_count` int DEFAULT 0 COMMENT '点赞数',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `unionidx_email_password` (`email`,`password`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `users_1` (user_id, username, email, password, create_time, update_time, follow_count, follower_count, avatar, background_image, signature, total_favorited, work_count, favorite_count) VALUES (1773577575179313154, '1111@qq.com', '1111@qq.com', 'dca86290bc8d422427405ac6ffe4b0e5', '2024-03-31 21:38:50', '2024-04-03 16:52:28', 1, 0, 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok_avatar.jpg', 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/background_image.jpg', '用户C', 0, 1, 0);
INSERT INTO `users_2` (user_id, username, email, password, create_time, update_time, follow_count, follower_count, avatar, background_image, signature, total_favorited, work_count, favorite_count) VALUES (1773596848777969665, '2323@qq.com', '2323@qq.com', 'dca86290bc8d422427405ac6ffe4b0e5', '2024-03-31 21:37:53', '2024-04-03 16:52:28', 1, 1, 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok_avatar.jpg', 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/background_image.jpg', '用户B', 1, 1, 0);
INSERT INTO `users_2` (user_id, username, email, password, create_time, update_time, follow_count, follower_count, avatar, background_image, signature, total_favorited, work_count, favorite_count) VALUES (1774363179018199041, '3232@qq.com', '3232@qq.com', 'dca86290bc8d422427405ac6ffe4b0e5', '2024-03-31 21:36:55', '2024-04-03 17:07:48', 1, 2, 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok_avatar.jpg', 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/background_image.jpg', '用户A', 1, 2, 2);


DROP TABLE IF EXISTS `user_follows_1`;
CREATE TABLE `user_follows_1` (
    `follow_id` bigint NOT NULL COMMENT '雪花算法生成id',
    `follower_id` bigint NOT NULL COMMENT '关注者id',
    `followed_id` bigint NOT NULL COMMENT '被关注者id',
    PRIMARY KEY (`follow_id`),
    UNIQUE KEY `idx_follower_followed` (`follower_id`,`followed_id`) USING BTREE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `user_follows_2`;
CREATE TABLE `user_follows_2` (
    `follow_id` bigint NOT NULL COMMENT '雪花算法生成id',
    `follower_id` bigint NOT NULL COMMENT '关注者id',
    `followed_id` bigint NOT NULL COMMENT '被关注者id',
    PRIMARY KEY (`follow_id`),
    UNIQUE KEY `idx_follower_followed` (`follower_id`,`followed_id`) USING BTREE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `user_follows_3`;
CREATE TABLE `user_follows_3` (
    `follow_id` bigint NOT NULL COMMENT '雪花算法生成id',
    `follower_id` bigint NOT NULL COMMENT '关注者id',
    `followed_id` bigint NOT NULL COMMENT '被关注者id',
    PRIMARY KEY (`follow_id`),
    UNIQUE KEY `idx_follower_followed` (`follower_id`,`followed_id`) USING BTREE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `user_follows_4`;
CREATE TABLE `user_follows_4` (
    `follow_id` bigint NOT NULL COMMENT '雪花算法生成id',
    `follower_id` bigint NOT NULL COMMENT '关注者id',
    `followed_id` bigint NOT NULL COMMENT '被关注者id',
    PRIMARY KEY (`follow_id`),
    UNIQUE KEY `idx_follower_followed` (`follower_id`,`followed_id`) USING BTREE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `user_follows_3` (follow_id, follower_id, followed_id) VALUES (1774437056232837542, 1773577575179313154, 1774363179018199041);
INSERT INTO `user_follows_2` (follow_id, follower_id, followed_id) VALUES (1774363428319252193, 1773596848777969665, 1774363179018199041);
INSERT INTO `user_follows_3` (follow_id, follower_id, followed_id) VALUES (1774437056230572034, 1774363179018199041, 1773596848777969665);


DROP TABLE IF EXISTS `video_1`;
CREATE TABLE `video_1` (
  `video_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `author_id` bigint NOT NULL COMMENT '视频对应作者id',
  `cover_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频封面对应地址',
  `play_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频对应地址',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间，自动填充',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间，自动更新',
  `title` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '抖音记录美好生活' COMMENT '视频标题',
  `favorite_count` int DEFAULT 0 COMMENT '视频点赞数',
  `comment_count` int DEFAULT 0 COMMENT '视频评论数',
  PRIMARY KEY (`video_id`),
  KEY `nidx_author_id` (`author_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `video_2`;
CREATE TABLE `video_2` (
  `video_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `author_id` bigint NOT NULL COMMENT '视频对应作者id',
  `cover_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频封面对应地址',
  `play_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频对应地址',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间，自动填充',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间，自动更新',
  `title` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '抖音记录美好生活' COMMENT '视频标题',
  `favorite_count` int DEFAULT 0 COMMENT '视频点赞数',
  `comment_count` int DEFAULT 0 COMMENT '视频评论数',
  PRIMARY KEY (`video_id`),
  KEY `nidx_author_id` (`author_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `video_3`;
CREATE TABLE `video_3` (
  `video_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `author_id` bigint NOT NULL COMMENT '视频对应作者id',
  `cover_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频封面对应地址',
  `play_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频对应地址',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间，自动填充',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间，自动更新',
  `title` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '抖音记录美好生活' COMMENT '视频标题',
  `favorite_count` int DEFAULT 0 COMMENT '视频点赞数',
  `comment_count` int DEFAULT 0 COMMENT '视频评论数',
  PRIMARY KEY (`video_id`),
  KEY `nidx_author_id` (`author_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `video_4`;
CREATE TABLE `video_4` (
  `video_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `author_id` bigint NOT NULL COMMENT '视频对应作者id',
  `cover_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频封面对应地址',
  `play_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频对应地址',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间，自动填充',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间，自动更新',
  `title` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '抖音记录美好生活' COMMENT '视频标题',
  `favorite_count` int DEFAULT 0 COMMENT '视频点赞数',
  `comment_count` int DEFAULT 0 COMMENT '视频评论数',
  PRIMARY KEY (`video_id`),
  KEY `nidx_author_id` (`author_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `video_5`;
CREATE TABLE `video_5` (
  `video_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `author_id` bigint NOT NULL COMMENT '视频对应作者id',
  `cover_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频封面对应地址',
  `play_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频对应地址',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间，自动填充',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间，自动更新',
  `title` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '抖音记录美好生活' COMMENT '视频标题',
  `favorite_count` int DEFAULT 0 COMMENT '视频点赞数',
  `comment_count` int DEFAULT 0 COMMENT '视频评论数',
  PRIMARY KEY (`video_id`),
  KEY `nidx_author_id` (`author_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `video_6`;
CREATE TABLE `video_6` (
  `video_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `author_id` bigint NOT NULL COMMENT '视频对应作者id',
  `cover_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频封面对应地址',
  `play_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频对应地址',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间，自动填充',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间，自动更新',
  `title` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '抖音记录美好生活' COMMENT '视频标题',
  `favorite_count` int DEFAULT 0 COMMENT '视频点赞数',
  `comment_count` int DEFAULT 0 COMMENT '视频评论数',
  PRIMARY KEY (`video_id`),
  KEY `nidx_author_id` (`author_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `video_7`;
CREATE TABLE `video_7` (
  `video_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `author_id` bigint NOT NULL COMMENT '视频对应作者id',
  `cover_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频封面对应地址',
  `play_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频对应地址',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间，自动填充',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间，自动更新',
  `title` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '抖音记录美好生活' COMMENT '视频标题',
  `favorite_count` int DEFAULT 0 COMMENT '视频点赞数',
  `comment_count` int DEFAULT 0 COMMENT '视频评论数',
  PRIMARY KEY (`video_id`),
  KEY `nidx_author_id` (`author_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `video_8`;
CREATE TABLE `video_8` (
  `video_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `author_id` bigint NOT NULL COMMENT '视频对应作者id',
  `cover_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频封面对应地址',
  `play_url` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '视频对应地址',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间，自动填充',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间，自动更新',
  `title` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '抖音记录美好生活' COMMENT '视频标题',
  `favorite_count` int DEFAULT 0 COMMENT '视频点赞数',
  `comment_count` int DEFAULT 0 COMMENT '视频评论数',
  PRIMARY KEY (`video_id`),
  KEY `nidx_author_id` (`author_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `video_2` (video_id, author_id, cover_url, play_url, create_time, update_time, title, favorite_count, comment_count) VALUES (1773578625353310209, 1774363179018199041, 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok/cover/2024-03-24T10%3A12%3A44.195978803f4-f52b-45ef-bf30-79c4c7e6ead2_cover.jpg', 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok/video/2024-03-24T10%3A12%3A46.366e045b588-7292-4d86-924e-af74e62da0e8_video.mp4', '2024-03-29 13:11:10', '2024-04-04 15:59:51', '用户A的视频', 0, 0);
INSERT INTO `video_3` (video_id, author_id, cover_url, play_url, create_time, update_time, title, favorite_count, comment_count) VALUES (1773579326334754818, 1773596848777969665, 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok/cover/2024-03-24T10%3A12%3A44.195978803f4-f52b-45ef-bf30-79c4c7e6ead2_cover.jpg', 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok/video/2024-03-24T10%3A12%3A46.366e045b588-7292-4d86-924e-af74e62da0e8_video.mp4', '2024-03-29 13:13:58', '2024-04-02 15:51:46', '用户B的视频', 1, 0);
INSERT INTO `video_3` (video_id, author_id, cover_url, play_url, create_time, update_time, title, favorite_count, comment_count) VALUES (1773579415144947714, 1773577575179313154, 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok/cover/2024-03-24T10%3A12%3A44.195978803f4-f52b-45ef-bf30-79c4c7e6ead2_cover.jpg', 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok/video/2024-03-24T10%3A12%3A46.366e045b588-7292-4d86-924e-af74e62da0e8_video.mp4', '2024-03-29 13:14:19', '2024-04-02 18:42:57', '用户C的视频', 0, 0);
INSERT INTO `video_3` (video_id, author_id, cover_url, play_url, create_time, update_time, title, favorite_count, comment_count) VALUES (1775447822198992898, 1774363179018199041, 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok/cover/2024-04-03T16:58:40.37123b77979-1305-4ab7-bfa7-166c46852008_cover.jpg', 'https://zzzi-img-1313100942.cos.ap-beijing.myqcloud.com/tiktok/video/2024-04-03T16:58:40.810aac4e952-c4d0-411d-8c45-e50d4a5011cc_video.mp4', '2024-04-03 16:58:42', '2024-04-03 17:07:48', '用户A4月3号下午投稿的视频', 1, 3);


DROP TABLE IF EXISTS `favorite_1`;
CREATE TABLE `favorite_1` (
  `favorite_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `user_id` bigint NOT NULL COMMENT '点赞的用户id',
  `video_id` bigint NOT NULL COMMENT '被点赞的视频id',
  PRIMARY KEY (`favorite_id`),
  UNIQUE KEY `index_user_id_video_id` (`user_id`,`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `favorite_2`;
CREATE TABLE `favorite_2` (
                              `favorite_id` bigint NOT NULL COMMENT '雪花算法生成id',
                              `user_id` bigint NOT NULL COMMENT '点赞的用户id',
                              `video_id` bigint NOT NULL COMMENT '被点赞的视频id',
                              PRIMARY KEY (`favorite_id`),
                              UNIQUE KEY `index_user_id_video_id` (`user_id`,`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `favorite_3`;
CREATE TABLE `favorite_3` (
                              `favorite_id` bigint NOT NULL COMMENT '雪花算法生成id',
                              `user_id` bigint NOT NULL COMMENT '点赞的用户id',
                              `video_id` bigint NOT NULL COMMENT '被点赞的视频id',
                              PRIMARY KEY (`favorite_id`),
                              UNIQUE KEY `index_user_id_video_id` (`user_id`,`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `favorite_4`;
CREATE TABLE `favorite_4` (
                              `favorite_id` bigint NOT NULL COMMENT '雪花算法生成id',
                              `user_id` bigint NOT NULL COMMENT '点赞的用户id',
                              `video_id` bigint NOT NULL COMMENT '被点赞的视频id',
                              PRIMARY KEY (`favorite_id`),
                              UNIQUE KEY `index_user_id_video_id` (`user_id`,`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `favorite_5`;
CREATE TABLE `favorite_5` (
                              `favorite_id` bigint NOT NULL COMMENT '雪花算法生成id',
                              `user_id` bigint NOT NULL COMMENT '点赞的用户id',
                              `video_id` bigint NOT NULL COMMENT '被点赞的视频id',
                              PRIMARY KEY (`favorite_id`),
                              UNIQUE KEY `index_user_id_video_id` (`user_id`,`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `favorite_6`;
CREATE TABLE `favorite_6` (
                              `favorite_id` bigint NOT NULL COMMENT '雪花算法生成id',
                              `user_id` bigint NOT NULL COMMENT '点赞的用户id',
                              `video_id` bigint NOT NULL COMMENT '被点赞的视频id',
                              PRIMARY KEY (`favorite_id`),
                              UNIQUE KEY `index_user_id_video_id` (`user_id`,`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `favorite_7`;
CREATE TABLE `favorite_7` (
                              `favorite_id` bigint NOT NULL COMMENT '雪花算法生成id',
                              `user_id` bigint NOT NULL COMMENT '点赞的用户id',
                              `video_id` bigint NOT NULL COMMENT '被点赞的视频id',
                              PRIMARY KEY (`favorite_id`),
                              UNIQUE KEY `index_user_id_video_id` (`user_id`,`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `favorite_8`;
CREATE TABLE `favorite_8` (
                              `favorite_id` bigint NOT NULL COMMENT '雪花算法生成id',
                              `user_id` bigint NOT NULL COMMENT '点赞的用户id',
                              `video_id` bigint NOT NULL COMMENT '被点赞的视频id',
                              PRIMARY KEY (`favorite_id`),
                              UNIQUE KEY `index_user_id_video_id` (`user_id`,`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `favorite_2` (favorite_id, user_id, video_id) VALUES (1775068179813990401, 1774363179018199041, 1773579326334754818);
INSERT INTO `favorite_3` (favorite_id, user_id, video_id) VALUES (1775450115027857410, 1774363179018199041, 1775447822198992898);


DROP TABLE IF EXISTS `comment_1`;
CREATE TABLE `comment_1` (
  `comment_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `user_id` bigint NOT NULL COMMENT '评论的用户id',
  `comment_text` varchar(1024) NOT NULL,
  `video_id` bigint NOT NULL COMMENT '被评论的视频id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间，自动填充',
  PRIMARY KEY (`comment_id`),
  KEY `idx_comment_user_id` (`user_id`) USING BTREE,
  KEY `idx_comment_vedio_id` (`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `comment_2`;
CREATE TABLE `comment_2` (
                             `comment_id` bigint NOT NULL COMMENT '雪花算法生成id',
                             `user_id` bigint NOT NULL COMMENT '评论的用户id',
                             `comment_text` varchar(1024) NOT NULL,
                             `video_id` bigint NOT NULL COMMENT '被评论的视频id',
                             `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间，自动填充',
                             PRIMARY KEY (`comment_id`),
                             KEY `idx_comment_user_id` (`user_id`) USING BTREE,
                             KEY `idx_comment_vedio_id` (`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `comment_3`;
CREATE TABLE `comment_3` (
                             `comment_id` bigint NOT NULL COMMENT '雪花算法生成id',
                             `user_id` bigint NOT NULL COMMENT '评论的用户id',
                             `comment_text` varchar(1024) NOT NULL,
                             `video_id` bigint NOT NULL COMMENT '被评论的视频id',
                             `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间，自动填充',
                             PRIMARY KEY (`comment_id`),
                             KEY `idx_comment_user_id` (`user_id`) USING BTREE,
                             KEY `idx_comment_vedio_id` (`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `comment_4`;
CREATE TABLE `comment_4` (
                             `comment_id` bigint NOT NULL COMMENT '雪花算法生成id',
                             `user_id` bigint NOT NULL COMMENT '评论的用户id',
                             `comment_text` varchar(1024) NOT NULL,
                             `video_id` bigint NOT NULL COMMENT '被评论的视频id',
                             `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间，自动填充',
                             PRIMARY KEY (`comment_id`),
                             KEY `idx_comment_user_id` (`user_id`) USING BTREE,
                             KEY `idx_comment_vedio_id` (`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `comment_5`;
CREATE TABLE `comment_5` (
                             `comment_id` bigint NOT NULL COMMENT '雪花算法生成id',
                             `user_id` bigint NOT NULL COMMENT '评论的用户id',
                             `comment_text` varchar(1024) NOT NULL,
                             `video_id` bigint NOT NULL COMMENT '被评论的视频id',
                             `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间，自动填充',
                             PRIMARY KEY (`comment_id`),
                             KEY `idx_comment_user_id` (`user_id`) USING BTREE,
                             KEY `idx_comment_vedio_id` (`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `comment_6`;
CREATE TABLE `comment_6` (
                             `comment_id` bigint NOT NULL COMMENT '雪花算法生成id',
                             `user_id` bigint NOT NULL COMMENT '评论的用户id',
                             `comment_text` varchar(1024) NOT NULL,
                             `video_id` bigint NOT NULL COMMENT '被评论的视频id',
                             `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间，自动填充',
                             PRIMARY KEY (`comment_id`),
                             KEY `idx_comment_user_id` (`user_id`) USING BTREE,
                             KEY `idx_comment_vedio_id` (`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `comment_7`;
CREATE TABLE `comment_7` (
                             `comment_id` bigint NOT NULL COMMENT '雪花算法生成id',
                             `user_id` bigint NOT NULL COMMENT '评论的用户id',
                             `comment_text` varchar(1024) NOT NULL,
                             `video_id` bigint NOT NULL COMMENT '被评论的视频id',
                             `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间，自动填充',
                             PRIMARY KEY (`comment_id`),
                             KEY `idx_comment_user_id` (`user_id`) USING BTREE,
                             KEY `idx_comment_vedio_id` (`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `comment_8`;
CREATE TABLE `comment_8` (
                             `comment_id` bigint NOT NULL COMMENT '雪花算法生成id',
                             `user_id` bigint NOT NULL COMMENT '评论的用户id',
                             `comment_text` varchar(1024) NOT NULL,
                             `video_id` bigint NOT NULL COMMENT '被评论的视频id',
                             `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间，自动填充',
                             PRIMARY KEY (`comment_id`),
                             KEY `idx_comment_user_id` (`user_id`) USING BTREE,
                             KEY `idx_comment_vedio_id` (`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `comment_2` (comment_id, user_id, comment_text, video_id, create_time) VALUES (1775768898669920257, 1774363179018199041, 'hhh', 1775447822198992898, '2024-04-04 14:14:32');
INSERT INTO `comment_3` (comment_id, user_id, comment_text, video_id, create_time) VALUES (1775775571052691458, 1773577575179313154, '你好你好', 1775447822198992898, '2024-04-04 14:41:03');
INSERT INTO `comment_3` (comment_id, user_id, comment_text, video_id, create_time) VALUES (1775777012316217346, 1773596848777969665, '测试测试', 1775447822198992898, '2024-04-04 14:46:47');


DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `message_id` bigint NOT NULL COMMENT '雪花算法生成id',
  `from_user_id` bigint NOT NULL COMMENT '发送方id',
  `to_user_id` bigint NOT NULL COMMENT '接收方id',
  `content` varchar(200) NOT NULL DEFAULT '嗨，一起来聊聊天吧' COMMENT '消息内容',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间，自动填充',
  PRIMARY KEY (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `message` (message_id, from_user_id, to_user_id, content, create_time) VALUES (1775791437383315457, 1774363179018199041, 1773596848777969665, '3232说你好', '2024-04-04 15:44:06');
INSERT INTO `message` (message_id, from_user_id, to_user_id, content, create_time) VALUES (1775799470888865794, 1774363179018199041, 1773596848777969665, '3232向B发送消息', '2024-04-04 16:16:01');
INSERT INTO `message` (message_id, from_user_id, to_user_id, content, create_time) VALUES (1775800672452104194, 1773596848777969665, 1774363179018199041, '用户B对A说你好3', '2024-04-04 16:20:48');


