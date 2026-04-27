package com.pengcheng.finance.commission.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.finance.commission.entity.CommissionRecord;
import com.pengcheng.finance.commission.entity.CommissionRule;

import java.math.BigDecimal;
import java.util.List;

/**
 * 通用提成规则与记录服务接口。
 * <p>
 * 适用于非房产行业（房产专属提成仍在 realty 模块的 {@code project_commission_rule}）。
 * 规则引擎（DSL 解析/阶梯计算）由 Phase 2 后续工单实现，当前实现返回占位异常。
 */
public interface CommissionRuleService {

    /**
     * 创建提成规则。
     *
     * @param rule 规则数据（ruleName、bizType、calcMode 必填）
     * @return 新规则 ID
     */
    Long createRule(CommissionRule rule);

    /**
     * 更新提成规则（仅修改配置，已生成的提成记录不受影响）。
     *
     * @param rule 包含 id 及变更字段
     */
    void updateRule(CommissionRule rule);

    /**
     * 启用或停用规则。
     *
     * @param id     规则 ID
     * @param active 1=启用 0=停用
     */
    void toggleActive(Long id, int active);

    /**
     * 逻辑删除规则。
     *
     * @param id 规则 ID
     */
    void deleteRule(Long id);

    /**
     * 按 ID 查询规则详情。
     *
     * @param id 规则 ID
     * @return 规则实体；不存在时返回 null
     */
    CommissionRule getById(Long id);

    /**
     * 按业务类型查询启用中的规则列表。
     *
     * @param bizType 业务类型（null 返回全部启用规则）
     * @return 规则列表
     */
    List<CommissionRule> listActiveByBizType(String bizType);

    /**
     * 分页查询规则列表（含已停用）。
     *
     * @param bizType  业务类型过滤（可为 null）
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     * @return 分页结果
     */
    IPage<CommissionRule> pageRules(String bizType, int pageNum, int pageSize);

    /**
     * 根据规则计算提成金额（不落库，仅返回计算结果）。
     * <p>
     * TODO Phase 2：实现固定比例/阶梯/团队/DSL 四种模式。
     *
     * @param ruleId     规则 ID
     * @param baseAmount 计算基数（通常为成交金额）
     * @return 应得提成金额
     * @throws UnsupportedOperationException 规则引擎 Phase 2 待实现
     */
    BigDecimal calculate(Long ruleId, BigDecimal baseAmount);

    /**
     * 触发提成计算并生成待审核记录。
     * <p>
     * 内部调用 {@link #calculate} 计算金额后写入 {@code commission_record}。
     *
     * @param ruleId     规则 ID
     * @param saleUserId 销售人员 user_id
     * @param bizId      关联业务 ID
     * @param bizType    业务类型
     * @param baseAmount 计算基数
     * @return 新生成的提成记录 ID
     * @throws UnsupportedOperationException 规则引擎 Phase 2 待实现
     */
    Long triggerCommission(Long ruleId, Long saleUserId, Long bizId, String bizType, BigDecimal baseAmount);

    /**
     * 审核通过提成记录。
     *
     * @param recordId   提成记录 ID
     * @param operatorId 审核人 user_id
     * @param remark     审核意见
     */
    void auditApprove(Long recordId, Long operatorId, String remark);

    /**
     * 审核拒绝提成记录。
     *
     * @param recordId   提成记录 ID
     * @param operatorId 审核人 user_id
     * @param remark     拒绝原因
     */
    void auditReject(Long recordId, Long operatorId, String remark);

    /**
     * 标记提成已发放。
     *
     * @param recordId   提成记录 ID
     * @param operatorId 操作人 user_id
     */
    void markPaid(Long recordId, Long operatorId);

    /**
     * 分页查询提成记录。
     *
     * @param saleUserId  销售人员 user_id 过滤（可为 null）
     * @param auditStatus 审核状态过滤（可为 null）
     * @param pageNum     页码（从 1 开始）
     * @param pageSize    每页条数
     * @return 分页结果
     */
    IPage<CommissionRecord> pageRecords(Long saleUserId, Integer auditStatus, int pageNum, int pageSize);
}
