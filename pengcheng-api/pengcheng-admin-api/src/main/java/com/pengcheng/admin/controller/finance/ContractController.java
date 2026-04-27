package com.pengcheng.admin.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.common.result.Result;
import com.pengcheng.finance.contract.entity.Contract;
import com.pengcheng.finance.contract.service.ContractService;
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
 * 合同管理 Controller stub（V4 Phase 2 骨架）。
 * <p>
 * 具体业务逻辑（审批流集成、e签宝/法大大对接）由 Phase 2 工单完成。
 * URL 前缀 {@code /admin/finance/contracts}。
 */
@RestController
@RequestMapping("/admin/finance/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    /**
     * 分页查询合同列表。
     *
     * @param customerId 客户 ID（可选）
     * @param status     合同状态（可选）
     * @param pageNum    页码，默认 1
     * @param pageSize   每页条数，默认 10
     */
    @GetMapping
    public Result<IPage<Contract>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(contractService.pageContracts(customerId, status, pageNum, pageSize));
    }

    /**
     * 查询合同详情。
     *
     * @param id 合同 ID
     */
    @GetMapping("/{id}")
    public Result<Contract> get(@PathVariable Long id) {
        return Result.ok(contractService.getById(id));
    }

    /**
     * 起草新合同。
     *
     * @param contract 合同数据（title、customerId 等必填，contractNo 由服务生成）
     */
    @PostMapping
    public Result<Long> create(@RequestBody Contract contract) {
        return Result.ok(contractService.draftContract(contract));
    }

    /**
     * 更新合同内容（自动生成新版本记录）。
     *
     * @param id       合同 ID
     * @param contract 更新数据
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Contract contract) {
        contract.setId(id);
        // TODO Phase 2：从 Sa-Token 获取操作人 ID
        contractService.updateContractContent(contract, null);
        return Result.ok();
    }

    /**
     * 作废合同（逻辑删除，状态变为已作废）。
     *
     * @param id 合同 ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // TODO Phase 2：从 Sa-Token 获取操作人 ID
        contractService.voidContract(id, null, "管理员操作");
        return Result.ok();
    }
}
