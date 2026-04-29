package com.pengcheng.system.plugin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.plugin.entity.TenantPlugin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 租户插件启用配置 Mapper。
 */
@Mapper
public interface TenantPluginMapper extends BaseMapper<TenantPlugin> {

    /**
     * 查询租户已启用的插件 code 列表。
     */
    @Select("SELECT plugin_code FROM tenant_plugin WHERE tenant_id = #{tenantId} AND enabled = 1")
    List<String> selectEnabledCodes(@Param("tenantId") Long tenantId);
}
