package com.zzzi.videoservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzzi.common.constant.RabbitMQKeys;
import com.zzzi.common.exception.CommentActionException;
import com.zzzi.common.feign.UserClient;
import com.zzzi.common.result.CommentVO;
import com.zzzi.common.result.UserInfoVO;
import com.zzzi.common.result.UserVO;
import com.zzzi.common.utils.JwtUtils;
import com.zzzi.videoservice.entity.CommentDO;
import com.zzzi.videoservice.mapper.CommentMapper;
import com.zzzi.videoservice.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author zzzi
 * @date 2024/4/3 22:13
 * 关于评论模块的全部操作
 * 评论不做缓存
 */
@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, CommentDO> implements CommentService {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private UserClient userClient;

    /**
     * @author zzzi
     * @date 2024/5/5 16:22
     * 新增父子评论
     */
    @Override
    public CommentVO commentParentAction(String token, String video_id, String comment_text, String parent_id, String reply_id) {
        log.info("用户评论操作service，token为：{}，video_id为：{}，父评论id为：{}", token, video_id, parent_id);
        //解析出用户的id
        Long userId = JwtUtils.getUserIdByToken(token);

        CommentDO commentDO = new CommentDO();
        commentDO.setUserId(userId);
        commentDO.setVideoId(Long.valueOf(video_id));
        commentDO.setCommentText(comment_text);
        //当前评论所属父评论的id，当前评论是父评论，对应的parent_id设置为0L
        boolean isFather = parent_id == null;
        String replyName = "";
        if (!isFather) {//子评论有一个回复名
            UserVO parent = userClient.userInfo(Long.valueOf(reply_id)).getUser();
            replyName = parent.getName();
        }
        commentDO.setParentId(isFather ? 0L : Long.valueOf(parent_id));
        commentDO.setReplyId(isFather ? 0L : Long.valueOf(reply_id));

        //评论表新增
        int insert = commentMapper.insert(commentDO);
        if (insert != 1) {//新增失败
            throw new CommentActionException("用户评论失败");
        }

        //发送异步消息，更新视频表的信息和缓存
        rabbitTemplate.convertAndSend(RabbitMQKeys.COMMENT_EXCHANGE, RabbitMQKeys.COMMENT_KEY, Long.valueOf(video_id));

        //返回包装的结果
        //远程调用获得当前评论用户的基本信息
        UserVO user = userClient.userInfo(userId).getUser();
        //刚新增的评论，一定没有子评论
        return packageFinalCommentVO(commentDO, user, null, replyName, isFather, userId, token);
    }

    /**
     * @author zzzi
     * @date 2024/5/5 16:23
     * 删除父子评论
     */
    @Override
    public CommentVO commentParentUnAction(String token, String video_id, String comment_id) {
        log.info("用户删除评论操作service，token为：{}，video_id为：{},comment_id为：{}", token, video_id, comment_id);
        //解析出用户的id
        Long userId = JwtUtils.getUserIdByToken(token);
        //删除对应的评论
        CommentDO commentDO = commentMapper.selectById(comment_id);
        /**@author zzzi
         * @date 2024/5/5 16:27
         * 当前评论是父评论，此时下面的所有子评论也需要删除
         */
        List<CommentDO> commentSonDOList = null;
        boolean isFather = commentDO.getParentId() == 0L;
        String replyName = "";
        if (isFather) {
            //查询当前父评论的所有子评论
            LambdaQueryWrapper<CommentDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CommentDO::getParentId, commentDO.getCommentId());
            //得到当前父评论的所有子评论
            commentSonDOList = commentMapper.selectList(queryWrapper);
            List<Long> idList = new ArrayList<>();
            for (CommentDO sonDo : commentSonDOList) {
                idList.add(sonDo.getCommentId());
            }
            //删除所有子评论
            commentMapper.deleteBatchIds(idList);
        } else {//当前评论是子评论，删除他的所有回复

            //查询当前被评论的作者信息
            UserInfoVO replyUserInfo = userClient.userInfo(commentDO.getReplyId());
            //得到父评论作者的姓名
            replyName = replyUserInfo.getUser().getName();
        }
        //删除这条父评论
        int delete = commentMapper.deleteById(commentDO);
        if (delete != 1) {
            throw new CommentActionException("用户删除评论失败");
        }

        //异步更新视频的评论数，包括数据库和缓存
        rabbitTemplate.convertAndSend(RabbitMQKeys.COMMENT_EXCHANGE, RabbitMQKeys.UN_COMMENT_KEY, Long.valueOf(video_id));
        //返回当前被删除的评论
        //远程调用获得当前评论用户的基本信息
        UserVO user = userClient.userInfo(commentDO.getUserId()).getUser();
        //获取当前用户的关注列表
        List<UserVO> user_list = userClient.getFollowList(userId.toString(), token).getUser_list();
        if (user_list.contains(user))//判断删评用户与当前登录用户的关注状态
            user.setIs_follow(true);
        return packageFinalCommentVO(commentDO, user, commentSonDOList, replyName, isFather, userId, token);
    }

    @Override
    public List<CommentVO> getParentCommentList(String token, String parent_id) {
        log.info("用户评论列表service，token为：{}，parent_id为：{}", token, parent_id);

        //先得到当前视频的所有评论信息
        //解析出用户的id
        Long userId = null;
        if (token != null && !"".equals(token)) {
            userId = JwtUtils.getUserIdByToken(token);
        }

        LambdaQueryWrapper<CommentDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentDO::getParentId, parent_id);
        //查询得到所有的子评论
        List<CommentDO> sonDOList = commentMapper.selectList(queryWrapper);
        return packageSonCommentList(sonDOList, userId, token);
    }

    /**
     * @author zzzi
     * @date 2024/5/5 19:33
     * 查询所有的父评论
     * 每个父评论查询其前三条评论返回，查看更多时会发一个请求，根据父评论id再查
     */
    @Override
    public List<CommentVO> getCommentList(String token, String video_id) {
        log.info("用户评论列表service，token为：{}，video_id为：{}", token, video_id);
        //先得到当前视频的所有评论信息
        //解析出用户的id
        Long userId = null;
        if (token != null && !"".equals(token)) {
            userId = JwtUtils.getUserIdByToken(token);
        }

        LambdaQueryWrapper<CommentDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentDO::getVideoId, video_id).eq(CommentDO::getParentId, 0L);
        //查询得到所有的父评论
        List<CommentDO> parentDOList = commentMapper.selectList(queryWrapper);

        return packageCommentListVO(parentDOList, userId, token);
    }


    /**
     * @author zzzi
     * @date 2024/4/4 13:21
     * 针对每个父评论，先获取三个子评论
     */
    private List<CommentVO> packageCommentListVO(List<CommentDO> commentDOList, Long userId, String token) {
        log.info("打包评论列表service,userId为：{}，token为：{}", userId, token);
        //获取当前userId对应的关注列表
        List<UserVO> followList = null;
        if (token != null && !"".equals(token))
            followList = userClient.getFollowList(userId.toString(), token).getUser_list();
        Map<Long, UserVO> userVOMap = new HashMap<>();
        List<CommentVO> comment_list = new ArrayList<>();
        //先判断是否有评论
        if (commentDOList == null || commentDOList.size() == 0) {
            return null;
        }
        //在这里说明是真的有
        for (CommentDO commentDO : commentDOList) {
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
            //每一个父评论查询前三条子评论
            LambdaQueryWrapper<CommentDO> sonQueryWrapper = new LambdaQueryWrapper<>();
            sonQueryWrapper.eq(CommentDO::getParentId, commentDO.getCommentId());
            Page<CommentDO> page = new Page<>(1, 3);
            page.addOrder(OrderItem.desc("update_time"));
            List<CommentDO> sonDoList = commentMapper.selectPage(page, sonQueryWrapper).getRecords();

            //有了commentDO和userVo以及子评论列表之后，打包成commentVO
            CommentVO commentVO = packageFinalCommentVO(commentDO, userVO, sonDoList, "", true, userId, token);
            comment_list.add(commentVO);
        }
        return comment_list;
    }

    private List<CommentVO> packageSonCommentList(List<CommentDO> sonDOList, Long userId, String token) {

        //获取当前userId对应的关注列表
        List<UserVO> followList = null;
        if (token != null && !"".equals(token))
            followList = userClient.getFollowList(userId.toString(), token).getUser_list();
        Map<Long, UserVO> userVOMap = new HashMap<>();

        List<CommentVO> sonVOList = new ArrayList<>();
        if (sonDOList == null)
            return sonVOList;
        //针对每一条子评论，判断关注关系
        for (CommentDO sonDO : sonDOList) {
            String replyName = userClient.userInfo(sonDO.getReplyId()).getUser().getName();
            //获取当前评论者的id
            Long sonDOUserId = sonDO.getUserId();
            //当前Map中有，可以直接复用
            UserVO userVO;
            if (userVOMap.containsKey(sonDOUserId)) {
                userVO = userVOMap.get(sonDOUserId);
            } else {//Map中没有，需要远程调用获取，并且判断关注关系
                userVO = userClient.userInfo(sonDOUserId).getUser();
                //已经登录的用户查看评论列表，已经关注时要设置关注状态
                if (token != null && followList != null && followList.contains(userVO)) {
                    userVO.setIs_follow(true);
                }
                //将其保存到Map中便于后期复用
                userVOMap.put(sonDOUserId, userVO);
            }
            CommentVO sonVO = packageSonCommentVO(sonDO, userVO, replyName);
            sonVOList.add(sonVO);
        }
        return sonVOList;
    }

    /**
     * @author zzzi
     * @date 2024/4/4 12:51
     * 打包评论实体类返回，主要是评论创建时间的格式：MM-dd
     */
    private CommentVO packageFinalCommentVO(CommentDO commentDO, UserVO user, List<CommentDO> sonDoList, String replyName, boolean isFather, Long userId, String token) {

        CommentVO commentVO = new CommentVO();
        if (isFather) {//父评论
            List<CommentVO> son_list = packageSonCommentList(sonDoList, userId, token);
            Date createTime = commentDO.getCreateTime();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
            String create_date = sdf.format(createTime);
            commentVO.setId(commentDO.getCommentId());
            commentVO.setUser(user);//父评论的作者
            commentVO.setContent(commentDO.getCommentText());
            commentVO.setCreate_date(create_date);
            //父评论需要打包子评论
            commentVO.setSon_list(son_list);
            commentVO.setIs_father(true);

        } else {//子评论
            commentVO = packageSonCommentVO(commentDO, user, replyName);
        }
        return commentVO;
    }

    //打包子评论的接口
    private CommentVO packageSonCommentVO(CommentDO commentDO, UserVO user, String replyName) {
        CommentVO commentVO = new CommentVO();
        commentVO.setReplyName(replyName);
        commentVO.setIs_father(false);
        commentVO.setId(commentDO.getCommentId());
        Date createTime = commentDO.getCreateTime();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
        String create_date = sdf.format(createTime);
        commentVO.setCreate_date(create_date);
        commentVO.setContent(commentDO.getCommentText());
        commentVO.setUser(user);
        return commentVO;
    }
}
