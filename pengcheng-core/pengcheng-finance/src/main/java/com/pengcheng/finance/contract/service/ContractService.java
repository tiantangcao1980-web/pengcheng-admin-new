package com.pengcheng.finance.contract.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.finance.contract.entity.Contract;
import com.pengcheng.finance.contract.entity.ContractSignRecord;
import com.pengcheng.finance.contract.entity.ContractVersion;

import java.util.List;

/**
 * 合同主流程服务接口。
 * <p>
 * 覆盖：起草 → 审批 → 在线签署（e签宝/法大大，Phase 2 后续对接）→ 履约跟踪 → 归档。
 */
public interface ContractService {

    /**
     * 起草合同（状态=起草，version=1）。
     *
     * @param contract 合同数据（contractNo 由服务生成，无需传入）
     * @return 新合同 ID
     */
    Long draftContract(Contract contract);

    /**
     * 提交合同审批（状态变为审批中）。
     *
     * @param contractId 合同 ID
     * @param operatorId 操作人 user_id
     */
    void submitForApproval(Long contractId, Long operatorId);

    /**
     * 审批通过（状态变为审批通过）。
     *
     * @param contractId 合同 ID
     * @param operatorId 审批人 user_id
     * @param remark     审批意见
     */
    void approveContract(Long contractId, Long operatorId, String remark);

    /**
     * 审批拒绝（状态变为审批拒绝）。
     *
     * @param contractId 合同 ID
     * @param operatorId 审批人 user_id
     * @param remark     拒绝原因
     */
    void rejectContract(Long contractId, Long operatorId, String remark);

    /**
     * 发起在线签署（状态变为签署中，调用外部 e签宝/法大大 API）。
     * <p>
     * TODO Phase 2：实现 e签宝/法大大 API 调用，填充 externalSignId。
     *
     * @param contractId   合同 ID
     * @param signProvider 签署服务商：esign / fadada
     * @param operatorId   操作人 user_id
     * @throws UnsupportedOperationException Phase 2 待实现
     */
    void initiateOnlineSign(Long contractId, String signProvider, Long operatorId);

    /**
     * 处理签署回调（外部平台推送签署结果时调用）。
     * <p>
     * TODO Phase 2：根据 externalSignId 匹配合同，更新签署状态和记录。
     *
     * @param externalSignId 外部平台合同 ID
     * @param signProvider   服务商标识
     * @param resultPayload  回调原始 JSON
     * @throws UnsupportedOperationException Phase 2 待实现
     */
    void handleSignCallback(String externalSignId, String signProvider, String resultPayload);

    /**
     * 标记合同进入履约阶段（状态变为履约中）。
     *
     * @param contractId 合同 ID
     * @param operatorId 操作人 user_id
     */
    void startPerforming(Long contractId, Long operatorId);

    /**
     * 归档合同（状态变为已归档）。
     *
     * @param contractId 合同 ID
     * @param operatorId 操作人 user_id
     */
    void archiveContract(Long contractId, Long operatorId);

    /**
     * 作废合同（状态变为已作废）。
     *
     * @param contractId 合同 ID
     * @param operatorId 操作人 user_id
     * @param reason     作废原因
     */
    void voidContract(Long contractId, Long operatorId, String reason);

    /**
     * 修改合同内容（自动生成新版本记录）。
     *
     * @param contract   更新后的合同数据（id 必填）
     * @param operatorId 操作人 user_id
     */
    void updateContractContent(Contract contract, Long operatorId);

    /**
     * 按 ID 查询合同详情。
     *
     * @param id 合同 ID
     * @return 合同实体；不存在时返回 null
     */
    Contract getById(Long id);

    /**
     * 分页查询合同列表。
     *
     * @param customerId 客户 ID 过滤（可为 null）
     * @param status     合同状态过滤（可为 null）
     * @param pageNum    页码（从 1 开始）
     * @param pageSize   每页条数
     * @return 分页结果
     */
    IPage<Contract> pageContracts(Long customerId, Integer status, int pageNum, int pageSize);

    /**
     * 查询合同的版本历史列表。
     *
     * @param contractId 合同 ID
     * @return 版本记录列表（按版本号升序）
     */
    List<ContractVersion> listVersions(Long contractId);

    /**
     * 查询合同的签署记录列表。
     *
     * @param contractId 合同 ID
     * @return 签署记录列表
     */
    List<ContractSignRecord> listSignRecords(Long contractId);
}
