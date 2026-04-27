package com.pengcheng.finance.contract.sign.esign.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * e签宝 Webhook 回调 JSON 解析 DTO。
 * <p>
 * e签宝回调 action 枚举（常用）：
 * <ul>
 *   <li>FlowFinish  — 整个签署流完成（所有人签完）</li>
 *   <li>FlowReject  — 签署流被拒绝</li>
 *   <li>FlowCancel  — 签署流被撤销</li>
 *   <li>SignFinish   — 单个签署人完成签署（多签时触发多次）</li>
 *   <li>FlowExpire  — 签署流已过期</li>
 * </ul>
 */
@Data
public class EsignCallback {

    /**
     * 事件动作类型（FlowFinish / FlowReject / FlowCancel / SignFinish / FlowExpire）。
     */
    private String action;

    /** e签宝签署流 ID（对应 contract.external_sign_id） */
    @JsonProperty("signFlowId")
    private String signFlowId;

    /**
     * 当前单个签署人完成签署时的签署人 ID（action=SignFinish 时有值）。
     * 用于匹配 contract_sign_record.external_sign_id。
     */
    @JsonProperty("signerId")
    private String signerId;

    /** 签署人手机号（action=SignFinish 时辅助匹配） */
    @JsonProperty("psnMobile")
    private String psnMobile;

    /** 签署完成时间（Unix 毫秒时间戳） */
    @JsonProperty("signTime")
    private Long signTime;

    /**
     * 拒绝 / 撤销原因（action=FlowReject 或 FlowCancel 时有值）。
     */
    private String reason;

    /**
     * e签宝回调事件唯一 ID（用于幂等去重）。
     * <p>
     * TODO：当前 contract_sign_record 表无 external_callback_id 字段，
     *       请在 V59 迁移中为 contract_sign_record 添加：
     *       <pre>
     *       ALTER TABLE contract_sign_record
     *           ADD COLUMN external_callback_id VARCHAR(128) COMMENT 'e签宝回调事件ID，幂等去重';
     *       </pre>
     *       目前幂等去重依赖 Redis：key = "esign:callback:{eventId}"，TTL 24h。
     */
    @JsonProperty("eventId")
    private String eventId;
}
