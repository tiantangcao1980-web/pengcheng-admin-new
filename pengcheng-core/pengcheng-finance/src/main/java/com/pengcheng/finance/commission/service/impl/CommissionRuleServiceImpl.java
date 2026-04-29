package com.pengcheng.finance.commission.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.finance.commission.entity.CommissionRecord;
import com.pengcheng.finance.commission.entity.CommissionRule;
import com.pengcheng.finance.commission.mapper.CommissionRecordMapper;
import com.pengcheng.finance.commission.mapper.CommissionRuleMapper;
import com.pengcheng.finance.commission.service.CommissionRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 通用提成规则服务实现（Phase 2 骨架占位）。
 * <p>
 * 规则引擎（固定比例/阶梯/团队/DSL）由 Phase 2 工单实现。
 * 房产行业专属提成（跳点/项目奖励）继续使用 realty 模块的 {@code project_commission_rule}，
 * 本实现不干预 realty 模块逻辑。
 */
@Service
@RequiredArgsConstructor
public class CommissionRuleServiceImpl implements CommissionRuleService {

    private final CommissionRuleMapper commissionRuleMapper;
    private final CommissionRecordMapper commissionRecordMapper;

    @Override
    public Long createRule(CommissionRule rule) {
        throw new UnsupportedOperationException("Phase 2 待实现：提成规则创建");
    }

    @Override
    public void updateRule(CommissionRule rule) {
        throw new UnsupportedOperationException("Phase 2 待实现：提成规则更新");
    }

    @Override
    public void toggleActive(Long id, int active) {
        throw new UnsupportedOperationException("Phase 2 待实现：提成规则启停");
    }

    @Override
    public void deleteRule(Long id) {
        throw new UnsupportedOperationException("Phase 2 待实现：提成规则删除");
    }

    @Override
    public CommissionRule getById(Long id) {
        throw new UnsupportedOperationException("Phase 2 待实现：提成规则查询");
    }

    @Override
    public List<CommissionRule> listActiveByBizType(String bizType) {
        throw new UnsupportedOperationException("Phase 2 待实现：提成规则列表查询");
    }

    @Override
    public IPage<CommissionRule> pageRules(String bizType, int pageNum, int pageSize) {
        throw new UnsupportedOperationException("Phase 2 待实现：提成规则分页查询");
    }

    @Override
    public BigDecimal calculate(Long ruleId, BigDecimal baseAmount) {
        // TODO Phase 2：根据 calcMode 分支调用不同计算策略
        //   - CALC_MODE_FIXED：baseAmount * rate
        //   - CALC_MODE_LADDER：遍历 ladder_json 匹配区间
        //   - CALC_MODE_TEAM：按 team_split_json 按比例分配
        //   - CALC_MODE_DSL：调用表达式引擎解析 expression_dsl
        throw new UnsupportedOperationException("Phase 2 待实现：提成规则引擎计算");
    }

    @Override
    public Long triggerCommission(Long ruleId, Long saleUserId, Long bizId, String bizType, BigDecimal baseAmount) {
        throw new UnsupportedOperationException("Phase 2 待实现：触发提成计算并生成记录");
    }

    @Override
    public void auditApprove(Long recordId, Long operatorId, String remark) {
        throw new UnsupportedOperationException("Phase 2 待实现：提成审核通过");
    }

    @Override
    public void auditReject(Long recordId, Long operatorId, String remark) {
        throw new UnsupportedOperationException("Phase 2 待实现：提成审核拒绝");
    }

    @Override
    public void markPaid(Long recordId, Long operatorId) {
        throw new UnsupportedOperationException("Phase 2 待实现：提成标记已发放");
    }

    @Override
    public IPage<CommissionRecord> pageRecords(Long saleUserId, Integer auditStatus, int pageNum, int pageSize) {
        throw new UnsupportedOperationException("Phase 2 待实现：提成记录分页查询");
    }
}
