package com.pengcheng.oa.shift.service;

import com.pengcheng.oa.shift.dto.ShiftEvaluationResult;
import com.pengcheng.oa.shift.entity.AttendanceShift;

import java.time.LocalDateTime;

/**
 * 班次规则引擎。
 * <p>
 * 抽象出"上班/下班打卡时间 + 班次模板 → 状态判定结果"的纯函数能力，
 * 可独立单测，不依赖任何 Spring 上下文。
 */
public interface ShiftRuleEngine {

    /**
     * 评估一对打卡时间是否合规。
     *
     * @param shift     班次模板
     * @param clockIn   上班打卡时间，可为 null
     * @param clockOut  下班打卡时间，可为 null
     * @return 评估结果
     */
    ShiftEvaluationResult evaluate(AttendanceShift shift, LocalDateTime clockIn, LocalDateTime clockOut);
}
