package com.pengcheng.realty.sop.service;

import com.pengcheng.realty.sop.dto.CommissionInitiateDTO;
import com.pengcheng.realty.sop.entity.RealtyCommissionTripartite;

import java.util.List;

/**
 * 佣金三方协议服务接口
 */
public interface CommissionTripartiteService {

    /**
     * 发起佣金三方协议签署流程（通常由成交回调触发）。
     * <ol>
     *   <li>幂等检查：同一 dealId 已存在则直接返回已有记录 ID</li>
     *   <li>计算佣金金额 = dealAmount × commissionRate</li>
     *   <li>渲染 commission_tripartite 模板</li>
     *   <li>生成文档，上传 OSS，写入 doc_url</li>
     *   <li>调用 EsignHttpClient.createSignFlow，sign_status 变为 SIGNING</li>
     * </ol>
     *
     * @param dto 发起参数
     * @return 三方协议记录 ID
     */
    Long initiate(CommissionInitiateDTO dto);

    /**
     * Webhook 回调或定时同步触发：将三方协议状态标记为 SIGNED。
     *
     * @param id 三方协议记录 ID
     */
    void onSigned(Long id);

    /**
     * 获取指定签署人的 H5 签署链接
     *
     * @param id       三方协议记录 ID
     * @param signerId e签宝签署人 ID
     * @return H5 跳转链接
     */
    String getSignUrl(Long id, String signerId);

    /**
     * 根据 ID 查询三方协议详情
     */
    RealtyCommissionTripartite getById(Long id);

    /**
     * 根据成交单 ID 查询三方协议
     */
    RealtyCommissionTripartite getByDealId(Long dealId);

    /**
     * 查询渠道的三方协议列表
     */
    List<RealtyCommissionTripartite> listByAlliance(Long allianceId);
}
