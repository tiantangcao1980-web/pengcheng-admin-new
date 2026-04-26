package com.pengcheng.crm.lead.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 线索分配/流转日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("crm_lead_assignment")
public class CrmLeadAssignment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long leadId;
    private Long fromUserId;
    private Long toUserId;
    private Long assignedBy;

    /** manual / round_robin / load_balance / rule */
    private String ruleType;

    private String note;

    private LocalDateTime createTime;
}
