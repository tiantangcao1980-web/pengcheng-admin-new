package com.pengcheng.finance.contract.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.finance.contract.entity.Contract;
import com.pengcheng.finance.contract.entity.ContractSignRecord;
import com.pengcheng.finance.contract.entity.ContractVersion;
import com.pengcheng.finance.contract.mapper.ContractMapper;
import com.pengcheng.finance.contract.mapper.ContractSignRecordMapper;
import com.pengcheng.finance.contract.mapper.ContractVersionMapper;
import com.pengcheng.finance.contract.service.ContractService;
import com.pengcheng.finance.contract.sign.esign.EsignHttpClient;
import com.pengcheng.finance.contract.sign.esign.dto.EsignCallback;
import com.pengcheng.finance.contract.sign.esign.dto.EsignSignFlowRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同主流程服务实现（Phase 2 骨架占位 + e签宝对接）。
 * <p>
 * 已实现：initiateOnlineSign / getSignUrl / handleSignCallback（esign 通道）。
 * 其余方法保持 Phase 2 骨架占位，后续工单落地。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractMapper contractMapper;
    private final ContractVersionMapper contractVersionMapper;
    private final ContractSignRecordMapper contractSignRecordMapper;

    /**
     * e签宝 HTTP 客户端（Feature Flag 关闭时为 null）。
     * <p>
     * 开启：{@code pengcheng.feature.esign=true}
     */
    @Autowired(required = false)
    private EsignHttpClient esignHttpClient;

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

    /**
     * 发起在线签署（e签宝通道）。
     * <p>
     * 流程：校验 Feature Flag → 查合同 → 构造签署流 → 调 e签宝 createSignFlow
     * → 回写 externalSignId + 设 sign_status=PROCESSING（STATUS_SIGNING）。
     * <p>
     * 非 esign 服务商当前仍抛出 UnsupportedOperationException。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initiateOnlineSign(Long contractId, String signProvider, Long operatorId) {
        if (!"esign".equals(signProvider)) {
            throw new UnsupportedOperationException("Phase 2 待实现：非 esign 签署服务商 " + signProvider);
        }
        requireEsignClient();

        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在 contractId=" + contractId);
        }

        // 构造最简签署流请求（文档 fileId 需提前上传到 e签宝，此处传占位）
        // TODO：生产场景应先调用 /v3/files/upload 上传 PDF，拿到 fileId 再传入
        EsignSignFlowRequest request = EsignSignFlowRequest.builder()
                .signFlowTitle(contract.getTitle())
                .notifyCallbackUrl(null) // 取全局 callbackUrl，由 EsignProperties 配置
                .docs(List.of(
                        EsignSignFlowRequest.EsignDocInfo.builder()
                                .fileId("PLACEHOLDER_FILE_ID") // Phase 2 TODO：替换为真实 fileId
                                .fileName(contract.getTitle() + ".pdf")
                                .build()
                ))
                .signers(List.of()) // Phase 2 TODO：从 contract_sign_record 预建签署人列表传入
                .build();

        String signFlowId = esignHttpClient.createSignFlow(request);
        log.info("[Esign] 签署流已创建 contractId={} signFlowId={}", contractId, signFlowId);

        // 回写 externalSignId 并更新合同状态为 SIGNING
        Contract update = new Contract();
        update.setId(contractId);
        update.setExternalSignId(signFlowId);
        update.setSignProvider("esign");
        update.setStatus(Contract.STATUS_SIGNING);
        contractMapper.updateById(update);
    }

    /**
     * 获取指定签署人的 H5 签署链接。
     * <p>
     * 调用 e签宝 GET /v3/sign-flow/{signFlowId}/sign-url 返回前端跳转链接。
     *
     * @param contractId 合同 ID
     * @param signerId   e签宝签署人 ID（contract_sign_record.external_sign_id 存储）
     * @return H5 签署链接
     */
    public String getSignUrl(Long contractId, Long signerId) {
        requireEsignClient();

        Contract contract = contractMapper.selectById(contractId);
        if (contract == null || contract.getExternalSignId() == null) {
            throw new IllegalStateException("合同不存在或未发起签署 contractId=" + contractId);
        }

        // 通过 contractId + signerId 查找对应的 external_sign_id（e签宝签署人 ID）
        ContractSignRecord record = contractSignRecordMapper.selectOne(
                new LambdaQueryWrapper<ContractSignRecord>()
                        .eq(ContractSignRecord::getContractId, contractId)
                        .eq(ContractSignRecord::getSignerId, signerId)
                        .last("LIMIT 1")
        );
        String esignSignerId = (record != null && record.getExternalSignId() != null)
                ? record.getExternalSignId()
                : String.valueOf(signerId); // fallback：直接用 signerId

        return esignHttpClient.getSignUrl(contract.getExternalSignId(), esignSignerId);
    }

    /**
     * 处理 e签宝 Webhook 回调，更新签署记录和合同状态。
     * <p>
     * 幂等：由调用方（EsignWebhookController）用 Redis 去重，保证同一 eventId 只处理一次。
     *
     * @param payload e签宝回调解析 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleSignCallback(EsignCallback payload) {
        String signFlowId = payload.getSignFlowId();
        String action = payload.getAction();
        log.info("[Esign] 收到回调 action={} signFlowId={} eventId={}", action, signFlowId, payload.getEventId());

        // 找到对应合同
        Contract contract = contractMapper.selectOne(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getExternalSignId, signFlowId)
                        .last("LIMIT 1")
        );
        if (contract == null) {
            log.warn("[Esign] 未找到对应合同 signFlowId={}", signFlowId);
            return;
        }

        switch (action) {
            case "SignFinish" -> {
                // 单个签署人完成签署 → 更新对应 sign_record
                if (payload.getSignerId() != null) {
                    ContractSignRecord record = contractSignRecordMapper.selectOne(
                            new LambdaQueryWrapper<ContractSignRecord>()
                                    .eq(ContractSignRecord::getContractId, contract.getId())
                                    .eq(ContractSignRecord::getExternalSignId, payload.getSignerId())
                                    .last("LIMIT 1")
                    );
                    if (record != null) {
                        ContractSignRecord update = new ContractSignRecord();
                        update.setId(record.getId());
                        update.setSignResult(ContractSignRecord.RESULT_SIGNED);
                        update.setSignTime(payload.getSignTime() != null
                                ? java.time.Instant.ofEpochMilli(payload.getSignTime())
                                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                                : LocalDateTime.now());
                        contractSignRecordMapper.updateById(update);
                    }
                }
                // 更新合同为部分签署
                Contract contractUpdate = new Contract();
                contractUpdate.setId(contract.getId());
                contractUpdate.setSignStatus(Contract.SIGN_STATUS_PARTIAL);
                contractMapper.updateById(contractUpdate);
            }
            case "FlowFinish" -> {
                // 所有人签完 → 合同状态变为 SIGNED，签署状态变为全部签署
                Contract contractUpdate = new Contract();
                contractUpdate.setId(contract.getId());
                contractUpdate.setStatus(Contract.STATUS_SIGNED);
                contractUpdate.setSignStatus(Contract.SIGN_STATUS_ALL);
                contractMapper.updateById(contractUpdate);
                log.info("[Esign] 合同全部签署完成 contractId={} signFlowId={}", contract.getId(), signFlowId);
            }
            case "FlowReject" -> {
                // 签署被拒绝 → 签署记录标记拒签，合同状态回到审批通过（等待重新发起）
                contractSignRecordMapper.selectList(
                        new LambdaQueryWrapper<ContractSignRecord>()
                                .eq(ContractSignRecord::getContractId, contract.getId())
                                .eq(ContractSignRecord::getSignResult, ContractSignRecord.RESULT_PENDING)
                ).forEach(r -> {
                    ContractSignRecord up = new ContractSignRecord();
                    up.setId(r.getId());
                    up.setSignResult(ContractSignRecord.RESULT_REFUSED);
                    contractSignRecordMapper.updateById(up);
                });
                Contract contractUpdate = new Contract();
                contractUpdate.setId(contract.getId());
                contractUpdate.setStatus(Contract.STATUS_APPROVED); // 回到审批通过，可重新发起
                contractMapper.updateById(contractUpdate);
            }
            case "FlowCancel", "FlowExpire" -> {
                // 撤销或过期 → 签署记录标记过期
                contractSignRecordMapper.selectList(
                        new LambdaQueryWrapper<ContractSignRecord>()
                                .eq(ContractSignRecord::getContractId, contract.getId())
                                .eq(ContractSignRecord::getSignResult, ContractSignRecord.RESULT_PENDING)
                ).forEach(r -> {
                    ContractSignRecord up = new ContractSignRecord();
                    up.setId(r.getId());
                    up.setSignResult(ContractSignRecord.RESULT_EXPIRED);
                    contractSignRecordMapper.updateById(up);
                });
                Contract contractUpdate = new Contract();
                contractUpdate.setId(contract.getId());
                contractUpdate.setStatus(Contract.STATUS_APPROVED);
                contractMapper.updateById(contractUpdate);
            }
            default -> log.warn("[Esign] 未知回调 action={}", action);
        }
    }

    /**
     * 原始接口方法（handleSignCallback 字符串版），保留以兼容接口签名。
     * e签宝通道请使用 {@link #handleSignCallback(EsignCallback)}。
     */
    @Override
    public void handleSignCallback(String externalSignId, String signProvider, String resultPayload) {
        if (!"esign".equals(signProvider)) {
            throw new UnsupportedOperationException("Phase 2 待实现：非 esign 签署服务商回调 " + signProvider);
        }
        // 由 Webhook Controller 解析后调用 handleSignCallback(EsignCallback)，此方法不直接使用
        throw new UnsupportedOperationException("请通过 EsignWebhookController 调用 handleSignCallback(EsignCallback)");
    }

    // =========================================================
    // 私有工具方法
    // =========================================================

    /**
     * 校验 e签宝 Feature Flag 是否开启，未开启时抛出 {@link IllegalStateException}。
     */
    private void requireEsignClient() {
        if (esignHttpClient == null) {
            throw new IllegalStateException(
                    "e签宝服务未启用，请在配置中设置 pengcheng.feature.esign=true 并配置 appId/appSecret");
        }
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
