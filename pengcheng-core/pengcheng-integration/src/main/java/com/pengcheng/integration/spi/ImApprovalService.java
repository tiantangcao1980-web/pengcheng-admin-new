package com.pengcheng.integration.spi;

import com.pengcheng.integration.spi.dto.ApprovalSyncEvent;

/**
 * IM 审批同步服务 SPI。
 * <p>
 * 将内部审批单推送到外部 IM 平台的审批流程。
 */
public interface ImApprovalService {

    /**
     * 推送审批事件到外部 IM 平台。
     *
     * @param tenantId 租户 ID
     * @param event    审批同步事件（含表单字段、模板 ID 等）
     */
    void pushApproval(Long tenantId, ApprovalSyncEvent event);
}
