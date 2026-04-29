package com.pengcheng.system.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.tenant.entity.TenantMemberInvite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户成员邀请 Mapper
 */
@Mapper
public interface TenantMemberInviteMapper extends BaseMapper<TenantMemberInvite> {
}
