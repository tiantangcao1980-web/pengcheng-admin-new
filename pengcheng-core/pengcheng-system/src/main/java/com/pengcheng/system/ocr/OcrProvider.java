package com.pengcheng.system.ocr;

import java.util.List;

/**
 * OCR 服务商抽象
 *
 * <p>由具体实现接入百度 / 腾讯 OCR；本接口不耦合任何 SDK，
 * 只需返回 OCR 文本行（按版面顺序），由 {@link BusinessCardParser}
 * 做字段抽取。</p>
 */
public interface OcrProvider {

    /**
     * 识别名片图片，返回 OCR 文本行
     *
     * @param imageBytes 图片字节
     * @return 文本行（按从上到下顺序）；识别失败返回空 List
     */
    List<String> recognize(byte[] imageBytes);

    /** 服务商类型，用于审计日志 */
    String getProviderType();
}
