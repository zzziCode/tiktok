package com.zzzi.videoservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzzi.common.result.CommentVO;
import com.zzzi.common.result.CommonVO;
import com.zzzi.videoservice.entity.CommentDO;

import java.util.List;

public interface CommentService extends IService<CommentDO> {
    List<CommentVO> getCommentList(String token, String video_id);

    CommentVO commentParentAction(String token, String video_id, String comment_text, String parent_id,String reply_id);

    CommentVO commentParentUnAction(String token, String video_id, String comment_id);

    List<CommentVO> getParentCommentList(String token, String parent_id);
}
