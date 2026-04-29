package com.pengcheng.crm.lead.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class LeadAssignDTO {

    @NotEmpty
    private List<Long> leadIds;

    /** manual / round_robin / load_balance / rule */
    private String ruleType;

    /** manual 时的目标用户 */
    private Long targetUserId;

    /** round_robin/load_balance 时候选人池 */
    private List<Long> candidateUserIds;

    private String note;
}
