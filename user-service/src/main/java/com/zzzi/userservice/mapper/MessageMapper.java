package com.zzzi.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzzi.userservice.entity.MessageDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<MessageDO> {
}
