package com.zzzi.videoservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzzi.common.result.CommentVO;
import com.zzzi.common.result.CommonVO;
import com.zzzi.videoservice.entity.CommentDO;

import java.util.List;

public interface CommentService extends IService<CommentDO> {
    CommentVO commentAction(String token, String video_id, String comment_text);

    CommentVO commentUnAction(String token, String video_id, String comment_id);

    List<CommentVO> getCommentList(String token, String video_id);
}
