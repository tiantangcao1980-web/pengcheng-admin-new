package com.pengcheng.oa.correction.service;

import com.pengcheng.oa.correction.dto.CorrectionApplyDTO;
import com.pengcheng.oa.correction.entity.AttendanceCorrection;

import java.util.List;

/**
 * 补卡单服务。
 * <p>
 * {@link #submit(CorrectionApplyDTO)} 在创建补卡单后会同步创建审批流实例。
 */
public interface AttendanceCorrectionService {

    Long submit(CorrectionApplyDTO dto);

    AttendanceCorrection getById(Long id);

    List<AttendanceCorrection> listByUser(Long userId, Integer status);

    /**
     * 当审批流终结时由 ApprovalFlow 引擎回调，同步状态。
     */
    void onApprovalComplete(Long correctionId, boolean approved);
}
