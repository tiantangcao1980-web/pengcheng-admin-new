package com.pengcheng.oa.flow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartInstanceDTO {

    /** 流程定义 ID（不传则取 bizType 的默认流程） */
    private Long flowDefId;

    /** 业务类型 */
    private String bizType;

    /** 业务实体 ID */
    private Long bizId;

    /** 申请人 */
    private Long applicantId;

    /** 摘要（用于待办列表展示） */
    private String summary;
}
