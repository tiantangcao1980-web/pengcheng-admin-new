package com.pengcheng.finance.contract.sign.esign;

import com.pengcheng.finance.contract.sign.esign.dto.EsignSignFlowRequest;

/**
 * e签宝 HTTP 客户端接口（依赖倒置，便于 Mock 测试）。
 * <p>
 * 所有方法在 HTTP 层或 e签宝业务错误时抛出 {@link EsignCallException}。
 */
public interface EsignHttpClient {

    /**
     * 创建签署流（POST /v3/sign-flow/create-by-doc）。
     *
     * @param request 签署流参数（包含文档信息和签署人列表）
     * @return e签宝返回的 signFlowId（签署流唯一标识）
     * @throws EsignCallException HTTP 调用失败或 e签宝返回业务错误码非 "000000"
     */
    String createSignFlow(EsignSignFlowRequest request);

    /**
     * 向已创建的签署流添加文档（POST /v3/sign-flow/{signFlowId}/docs）。
     * <p>
     * 注意：createSignFlow 已支持在创建时携带 docs，此方法用于补充添加场景。
     *
     * @param signFlowId e签宝签署流 ID
     * @param fileId     e签宝文件 ID（提前通过 /v3/files/upload 上传获得）
     * @param fileName   文件名（含扩展名）
     * @throws EsignCallException HTTP 调用失败
     */
    void addDoc(String signFlowId, String fileId, String fileName);

    /**
     * 获取指定签署人的 H5 签署链接（GET /v3/sign-flow/{signFlowId}/sign-url）。
     *
     * @param signFlowId e签宝签署流 ID
     * @param signerId   e签宝签署人 ID（创建签署流时 e签宝返回）
     * @return H5 跳转链接（有效期通常 30 分钟，前端跳转使用）
     * @throws EsignCallException HTTP 调用失败
     */
    String getSignUrl(String signFlowId, String signerId);

    /**
     * 查询签署流状态（GET /v3/sign-flow/{signFlowId}/detail）。
     *
     * @param signFlowId e签宝签署流 ID
     * @return 签署流状态字符串（DRAFT / SIGNING / DONE / REJECTED / CANCELED / EXPIRED）
     * @throws EsignCallException HTTP 调用失败
     */
    String queryFlowStatus(String signFlowId);
}
