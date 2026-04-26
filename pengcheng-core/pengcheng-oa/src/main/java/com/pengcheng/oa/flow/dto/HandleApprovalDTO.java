package com.pengcheng.oa.flow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HandleApprovalDTO {

    /** 流程实例 ID */
    private Long instanceId;

    /** 处理人 */
    private Long approverId;

    /** 是否通过 */
    private Boolean approved;

    /** 备注 */
    private String remark;
}
