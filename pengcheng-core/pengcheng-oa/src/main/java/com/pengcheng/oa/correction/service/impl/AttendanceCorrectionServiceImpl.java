package com.pengcheng.oa.correction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.oa.correction.dto.CorrectionApplyDTO;
import com.pengcheng.oa.correction.entity.AttendanceCorrection;
import com.pengcheng.oa.correction.mapper.AttendanceCorrectionMapper;
import com.pengcheng.oa.correction.service.AttendanceCorrectionService;
import com.pengcheng.oa.flow.dto.StartInstanceDTO;
import com.pengcheng.oa.flow.service.ApprovalFlowCallback;
import com.pengcheng.oa.flow.service.ApprovalFlowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceCorrectionServiceImpl implements AttendanceCorrectionService, ApprovalFlowCallback {

    public static final String BIZ_TYPE = "correction";

    private final AttendanceCorrectionMapper correctionMapper;
    private final ApprovalFlowEngine flowEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(CorrectionApplyDTO dto) {
        if (dto.getUserId() == null) throw new IllegalArgumentException("申请人ID不能为空");
        if (dto.getCorrectionDate() == null) throw new IllegalArgumentException("补卡日期不能为空");
        if (dto.getCorrectionType() == null) throw new IllegalArgumentException("补卡类型不能为空");
        if (dto.getExpectedTime() == null) throw new IllegalArgumentException("应打卡时间不能为空");

        AttendanceCorrection correction = new AttendanceCorrection();
        correction.setUserId(dto.getUserId());
        correction.setCorrectionDate(dto.getCorrectionDate());
        correction.setCorrectionType(dto.getCorrectionType());
        correction.setExpectedTime(dto.getExpectedTime());
        correction.setReason(dto.getReason());
        correction.setStatus(AttendanceCorrection.STATUS_PENDING);
        correctionMapper.insert(correction);

        // 启动审批流
        StartInstanceDTO startDto = StartInstanceDTO.builder()
                .flowDefId(dto.getFlowDefId())
                .bizType(BIZ_TYPE)
                .bizId(correction.getId())
                .applicantId(dto.getUserId())
                .summary("补卡：" + dto.getCorrectionDate() + " "
                        + (dto.getCorrectionType() == AttendanceCorrection.CORRECTION_TYPE_CLOCK_IN ? "上班" : "下班"))
                .build();
        Long instanceId = flowEngine.start(startDto);

        correction.setApprovalInstanceId(instanceId);
        correctionMapper.updateById(correction);
        return correction.getId();
    }

    @Override
    public AttendanceCorrection getById(Long id) {
        return correctionMapper.selectById(id);
    }

    @Override
    public List<AttendanceCorrection> listByUser(Long userId, Integer status) {
        LambdaQueryWrapper<AttendanceCorrection> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(AttendanceCorrection::getUserId, userId);
        if (status != null) wrapper.eq(AttendanceCorrection::getStatus, status);
        wrapper.orderByDesc(AttendanceCorrection::getCreateTime);
        return correctionMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalComplete(Long correctionId, boolean approved) {
        AttendanceCorrection correction = correctionMapper.selectById(correctionId);
        if (correction == null) {
            return;
        }
        correction.setStatus(approved ? AttendanceCorrection.STATUS_APPROVED : AttendanceCorrection.STATUS_REJECTED);
        correctionMapper.updateById(correction);
    }

    // ===== ApprovalFlowCallback =====

    @Override
    public String supportBizType() {
        return BIZ_TYPE;
    }

    @Override
    public void onComplete(Long bizId, boolean approved) {
        onApprovalComplete(bizId, approved);
    }
}
