package com.zzzi.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzzi.userservice.entity.UserFollowDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RelationMapper extends BaseMapper<UserFollowDO> {
}
