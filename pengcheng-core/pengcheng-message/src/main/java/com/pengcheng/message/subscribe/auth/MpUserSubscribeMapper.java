package com.pengcheng.message.subscribe.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 小程序订阅授权记录 Mapper
 */
@Mapper
public interface MpUserSubscribeMapper extends BaseMapper<MpUserSubscribe> {

    /**
     * 原子消费一次配额：在 used < quota 且 revoked = 0 时将 used +1。
     *
     * @param userId     用户 ID
     * @param templateId 模板 ID
     * @return 更新行数（1=成功，0=配额不足或已撤销）
     */
    @Update("UPDATE mp_user_subscribe " +
            "SET used = used + 1, update_time = NOW() " +
            "WHERE user_id = #{userId} AND template_id = #{templateId} " +
            "  AND used < quota AND revoked = 0")
    int consumeQuota(@Param("userId") Long userId, @Param("templateId") String templateId);
}
