package com.pengcheng.message.channel;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * push_channel_log 表 MyBatis-Plus Mapper（V4 MVP F4）
 *
 * <p>对应 V55 (原 V22) push_channel_log 表，由 {@link DbPushChannelLogStore} 调用。
 */
@Mapper
public interface PushChannelLogMapper extends BaseMapper<PushChannelLog> {
}
