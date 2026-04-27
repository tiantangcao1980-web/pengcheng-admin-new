package com.pengcheng.realty.sop.service;

import com.pengcheng.realty.sop.dto.VisitSopCreateDTO;
import com.pengcheng.realty.sop.entity.RealtyVisitSop;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 带看 SOP 服务接口
 */
public interface VisitSopService {

    /**
     * 发起带看确认书签署流程。
     * <ol>
     *   <li>创建 SOP 记录，status=PENDING_CONFIRM</li>
     *   <li>渲染 visit_confirm 模板</li>
     *   <li>生成文档字节（简化版：HTML），上传 OSS，写入 confirm_doc_url</li>
     *   <li>调用 EsignHttpClient.createSignFlow 创建签署流，写入 confirm_sign_id</li>
     * </ol>
     *
     * @param dto 带看信息
     * @return 新建 SOP 记录 ID
     */
    Long initiate(VisitSopCreateDTO dto);

    /**
     * 获取指定签署人的 H5 签署链接（调用 e签宝）。
     *
     * @param sopId    SOP 记录 ID
     * @param signerId e签宝签署人 ID（由 e签宝分配，客户端可从创建流程响应中获取）
     * @return H5 跳转链接，有效期约 30 分钟
     */
    String getSignUrl(Long sopId, String signerId);

    /**
     * Webhook 回调或定时同步触发：将 SOP 状态标记为 CONFIRMED。
     *
     * @param sopId SOP 记录 ID
     */
    void onSigned(Long sopId);

    /**
     * 风控查询：判断该客户在指定时间点是否被某个未过期且已确认的 SOP 覆盖。
     * <p>
     * 成交分佣前置校验：若返回 false，说明无有效带看确认书，应拒绝发起佣金三方单。
     *
     * @param customerId 客户 ID
     * @param allianceId 渠道联盟商 ID
     * @param time       检查时间点（通常为成交时间）
     * @return true — 存在有效 SOP 覆盖；false — 无有效覆盖
     */
    boolean isCovered(Long customerId, Long allianceId, LocalDateTime time);

    /**
     * 根据 ID 查询 SOP 详情
     */
    RealtyVisitSop getById(Long id);

    /**
     * 查询客户的带看 SOP 列表
     */
    List<RealtyVisitSop> listByCustomer(Long customerId);
}
