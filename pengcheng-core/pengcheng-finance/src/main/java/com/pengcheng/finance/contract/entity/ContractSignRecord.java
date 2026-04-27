package com.pengcheng.finance.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合同签署记录（contract_sign_record）。
 * <p>
 * 记录每位签署方的签署动作，支持多签署人场景（甲方、乙方、见证方等）。
 */
@Data
@TableName("contract_sign_record")
public class ContractSignRecord {

    // ==================== 签署结果常量 ====================
    public static final int RESULT_PENDING  = 0; // 待签
    public static final int RESULT_SIGNED   = 1; // 已签
    public static final int RESULT_REFUSED  = 2; // 已拒签
    public static final int RESULT_EXPIRED  = 3; // 已过期

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 合同 ID（contract.id） */
    private Long contractId;

    /** 签署人系统 user_id（内部用户；外部签署人为 null） */
    private Long signerId;

    /** 签署人姓名 */
    private String signerName;

    /** 签署方角色：partyA=甲方 / partyB=乙方 / witness=见证方 */
    private String signerRole;

    /** 签署完成时间 */
    private LocalDateTime signTime;

    /** 签署服务商：esign / fadada / offline */
    private String signProvider;

    /** 外部签署平台的签署记录 ID */
    private String externalSignId;

    /**
     * 签署结果。
     *
     * @see #RESULT_PENDING
     * @see #RESULT_SIGNED
     */
    private Integer signResult;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
