package com.pengcheng.oa.flow.dto;

import com.pengcheng.oa.flow.entity.ApprovalInstance;
import com.pengcheng.oa.flow.entity.ApprovalRecord;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InstanceDetailVO {

    private ApprovalInstance instance;

    private List<ApprovalRecord> records;
}
