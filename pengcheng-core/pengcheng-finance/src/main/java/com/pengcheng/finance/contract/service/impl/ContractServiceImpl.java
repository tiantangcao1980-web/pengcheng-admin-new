package com.pengcheng.finance.contract.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.finance.contract.entity.Contract;
import com.pengcheng.finance.contract.entity.ContractSignRecord;
import com.pengcheng.finance.contract.entity.ContractVersion;
import com.pengcheng.finance.contract.mapper.ContractMapper;
import com.pengcheng.finance.contract.mapper.ContractSignRecordMapper;
import com.pengcheng.finance.contract.mapper.ContractVersionMapper;
import com.pengcheng.finance.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 合同主流程服务实现（Phase 2 骨架占位）。
 * <p>
 * e签宝/法大大 API 对接、审批流集成等具体逻辑由 Phase 2 工单落地。
 */
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractMapper contractMapper;
    private final ContractVersionMapper contractVersionMapper;
    private final ContractSignRecordMapper contractSignRecordMapper;

    @Override
    public Long draftContract(Contract contract) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同起草");
    }

    @Override
    public void submitForApproval(Long contractId, Long operatorId) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同提交审批");
    }

    @Override
    public void approveContract(Long contractId, Long operatorId, String remark) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同审批通过");
    }

    @Override
    public void rejectContract(Long contractId, Long operatorId, String remark) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同审批拒绝");
    }

    @Override
    public void initiateOnlineSign(Long contractId, String signProvider, Long operatorId) {
        // TODO Phase 2：调用 e签宝（esign）或法大大（fadada）API 发起签署流程
        throw new UnsupportedOperationException("Phase 2 待实现：发起在线签署（e签宝/法大大 API）");
    }

    @Override
    public void handleSignCallback(String externalSignId, String signProvider, String resultPayload) {
        // TODO Phase 2：解析回调 JSON，更新 contract.sign_status 和 contract_sign_record
        throw new UnsupportedOperationException("Phase 2 待实现：签署平台回调处理");
    }

    @Override
    public void startPerforming(Long contractId, Long operatorId) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同进入履约");
    }

    @Override
    public void archiveContract(Long contractId, Long operatorId) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同归档");
    }

    @Override
    public void voidContract(Long contractId, Long operatorId, String reason) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同作废");
    }

    @Override
    public void updateContractContent(Contract contract, Long operatorId) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同内容修改（版本递增）");
    }

    @Override
    public Contract getById(Long id) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同详情查询");
    }

    @Override
    public IPage<Contract> pageContracts(Long customerId, Integer status, int pageNum, int pageSize) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同分页列表");
    }

    @Override
    public List<ContractVersion> listVersions(Long contractId) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同版本历史");
    }

    @Override
    public List<ContractSignRecord> listSignRecords(Long contractId) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同签署记录查询");
    }
}
