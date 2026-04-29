package com.pengcheng.integration.spi.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * 审批同步事件（由内部审批推送至 IM 平台）。
 */
@Data
@Accessors(chain = true)
public class ApprovalSyncEvent {

    /** 内部审批单 ID */
    private Long approvalId;

    /** 审批流程名称（在 IM 平台的 spname） */
    private String spName;

    /** 外部审批模板 ID（企业微信需提前配置） */
    private String templateId;

    /** 发起人外部 ID（企业微信 userId） */
    private String applicantExternalId;

    /**
     * 审批表单字段列表。
     * key: 字段控件 ID，value: 字段值（字符串表示）
     */
    private List<Map<String, String>> fields;

    /** 回调摘要 URL（审批结果通知跳转） */
    private String notifyUrl;
}
