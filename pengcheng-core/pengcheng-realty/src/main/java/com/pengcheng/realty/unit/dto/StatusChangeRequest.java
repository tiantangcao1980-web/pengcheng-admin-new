package com.pengcheng.realty.unit.dto;

import lombok.Data;

/**
 * 房源状态变更请求
 */
@Data
public class StatusChangeRequest {

    /** 目标状态 */
    private String toStatus;

    /** 操作人 ID */
    private Long operatorId;

    /** 关联客户 */
    private Long customerId;

    /** 关联成交单 */
    private Long dealId;

    /** 变更原因 */
    private String reason;
}
