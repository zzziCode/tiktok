package com.zzzi.videoservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzzi.videoservice.entity.FavoriteDO;
import com.zzzi.common.result.VideoVO;

import java.util.List;

public interface FavoriteService extends IService<FavoriteDO> {
    void favoriteAction(String token, String video_id);

    void favoriteUnAction(String token, String video_id);

    List<VideoVO> getFavoriteList(String user_id, String token);
}
