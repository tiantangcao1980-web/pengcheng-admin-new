package com.pengcheng.system.dashboard.service;

import com.pengcheng.system.dashboard.dto.CardRenderResponse;
import com.pengcheng.system.dashboard.dto.RenderRequest;
import com.pengcheng.system.dashboard.entity.DashboardCardDef;

import java.util.List;
import java.util.Set;

/**
 * 看板卡片服务接口。
 */
public interface DashboardCardService {

    /**
     * 渲染指定卡片。
     *
     * <p>异常隔离：若 provider 抛出任何异常，不向上传播，
     * 而是在响应中置 {@code success=false} 并附带 error 消息。
     *
     * @param code    卡片代码
     * @param request 渲染请求（含时间窗口、附加参数）
     * @param userId  当前用户 ID
     * @param tenantId 当前租户 ID
     * @return 卡片渲染响应
     */
    CardRenderResponse renderCard(String code, RenderRequest request,
                                  Long userId, Long tenantId);

    /**
     * 列出所有已启用的卡片定义。
     */
    List<DashboardCardDef> listAvailable();

    /**
     * 按角色过滤可见的卡片定义。
     *
     * @param roles 当前用户的角色集合
     */
    List<DashboardCardDef> listForRoles(Set<String> roles);
}
