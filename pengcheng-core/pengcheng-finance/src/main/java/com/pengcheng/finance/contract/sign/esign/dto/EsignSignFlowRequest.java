package com.pengcheng.finance.contract.sign.esign.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 创建 e签宝签署流参数（对应 POST /v3/sign-flow/create-by-doc 请求体）。
 * <p>
 * 字段命名与 e签宝 API v3 JSON key 保持一致，使用 Jackson 序列化。
 */
@Data
@Builder
public class EsignSignFlowRequest {

    /** 签署流名称（通常为合同标题） */
    private String signFlowTitle;

    /** 签署截止时间戳（Unix ms），不传默认 30 天 */
    private Long signFlowExpireTime;

    /** 签署完成回调 URL，覆盖全局配置 */
    private String notifyCallbackUrl;

    /**
     * 文档列表（至少一个）。
     * 若已在 e签宝上传过文档可直接传 fileId；
     * 若需上传可传 base64 内容（由 addDoc 接口处理）。
     */
    private List<EsignDocInfo> docs;

    /** 签署人列表（甲方、乙方等） */
    private List<EsignSigner> signers;

    // ---- 内部嵌套类 ----

    @Data
    @Builder
    public static class EsignDocInfo {
        /** e签宝文件 ID（调用 /v3/files/upload 后获得） */
        private String fileId;
        /** 文件名称（含扩展名，如 合同.pdf） */
        private String fileName;
    }
}
