package com.pengcheng.admin.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.common.result.Result;
import com.pengcheng.finance.commission.entity.CommissionRecord;
import com.pengcheng.finance.commission.entity.CommissionRule;
import com.pengcheng.finance.commission.service.CommissionRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用销售提成规则 Controller stub（V4 Phase 2 骨架）。
 * <p>
 * 规则引擎（DSL 解析/阶梯计算）由 Phase 2 工单完成。
 * 注意：房产行业专属提成见 realty 模块，本 Controller 仅管理通用规则。
 * URL 前缀 {@code /admin/finance/commission-rules}。
 */
@RestController
@RequestMapping("/admin/finance/commission-rules")
@RequiredArgsConstructor
public class CommissionRuleController {

    private final CommissionRuleService commissionRuleService;

    /**
     * 分页查询提成规则列表。
     *
     * @param bizType  业务类型过滤（可选）
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 10
     */
    @GetMapping
    public Result<IPage<CommissionRule>> list(
            @RequestParam(required = false) String bizType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(commissionRuleService.pageRules(bizType, pageNum, pageSize));
    }

    /**
     * 查询规则详情。
     *
     * @param id 规则 ID
     */
    @GetMapping("/{id}")
    public Result<CommissionRule> get(@PathVariable Long id) {
        return Result.ok(commissionRuleService.getById(id));
    }

    /**
     * 创建提成规则。
     *
     * @param rule 规则数据（ruleName、bizType、calcMode 必填）
     */
    @PostMapping
    public Result<Long> create(@RequestBody CommissionRule rule) {
        return Result.ok(commissionRuleService.createRule(rule));
    }

    /**
     * 更新提成规则。
     *
     * @param id   规则 ID
     * @param rule 更新数据
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody CommissionRule rule) {
        rule.setId(id);
        commissionRuleService.updateRule(rule);
        return Result.ok();
    }

    /**
     * 停用规则（逻辑删除）。
     *
     * @param id 规则 ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commissionRuleService.deleteRule(id);
        return Result.ok();
    }

    /**
     * 分页查询提成记录（附在规则 Controller 下便于管理查看）。
     *
     * @param saleUserId  销售人员 user_id 过滤（可选）
     * @param auditStatus 审核状态过滤（可选）
     * @param pageNum     页码，默认 1
     * @param pageSize    每页条数，默认 10
     */
    @GetMapping("/records")
    public Result<IPage<CommissionRecord>> listRecords(
            @RequestParam(required = false) Long saleUserId,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(commissionRuleService.pageRecords(saleUserId, auditStatus, pageNum, pageSize));
    }
}
