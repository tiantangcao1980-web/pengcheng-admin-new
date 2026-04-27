package com.pengcheng.system.ocr.integration;

import com.pengcheng.system.ocr.BusinessCardData;
import com.pengcheng.system.ocr.BusinessCardOcrService;
import com.pengcheng.system.ocr.baidu.OcrCallException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 名片导入客户集成 Service
 *
 * <p>职责：
 * <ol>
 *   <li>将 Base64 图片解码为字节数组</li>
 *   <li>调用 {@link BusinessCardOcrService} 识别名片</li>
 *   <li>将 {@link BusinessCardData} 字段映射到 {@link CustomerImportPreview}</li>
 *   <li>构建 rawFields 供前端展示置信度</li>
 * </ol>
 *
 * <p>异常处理：
 * <ul>
 *   <li>空/null 图片 → {@link IllegalArgumentException}</li>
 *   <li>OCR 调用失败（{@link OcrCallException}）→ 包装为 {@link CardPreviewException}</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardImportToCustomerService {

    private final BusinessCardOcrService businessCardOcrService;

    /**
     * 识别名片图片并返回客户字段预览。
     *
     * @param imageBase64 Base64 编码的图片字节（不含 data URI 前缀）
     * @return 客户字段预览 DTO
     * @throws IllegalArgumentException 入参为空时
     * @throws CardPreviewException     OCR 调用失败时
     */
    public CustomerImportPreview previewFromImage(String imageBase64) {
        if (imageBase64 == null || imageBase64.isBlank()) {
            throw new IllegalArgumentException("imageBase64 不能为空");
        }

        // 兼容 data URI 前缀（如 "data:image/jpeg;base64,..."）
        String base64Data = imageBase64;
        int commaIdx = imageBase64.indexOf(',');
        if (commaIdx >= 0) {
            base64Data = imageBase64.substring(commaIdx + 1);
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(base64Data.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("imageBase64 格式不合法: " + e.getMessage(), e);
        }

        if (imageBytes.length == 0) {
            throw new IllegalArgumentException("imageBase64 解码后为空字节");
        }

        BusinessCardData cardData;
        try {
            cardData = businessCardOcrService.recognize(imageBytes);
        } catch (OcrCallException e) {
            log.warn("OCR 调用失败: {}", e.getMessage());
            throw new CardPreviewException("名片识别失败，请检查图片后重试: " + e.getMessage(), e);
        }

        return toPreview(cardData);
    }

    /**
     * 将 OCR 识别结果映射为前端预填 DTO。
     *
     * <p>字段映射规则：
     * <ul>
     *   <li>mobile → phone（手机号优先，去非数字字符后取第一条）</li>
     *   <li>name → name</li>
     *   <li>email → email（已小写）</li>
     *   <li>company → company</li>
     *   <li>position → position</li>
     *   <li>address → address</li>
     *   <li>telephone → telephone（座机，辅助展示）</li>
     *   <li>website → website（辅助展示）</li>
     * </ul>
     */
    private CustomerImportPreview toPreview(BusinessCardData data) {
        Map<String, String> rawFields = buildRawFields(data);

        return CustomerImportPreview.builder()
                .name(data.getName())
                .phone(data.getMobile())
                .email(data.getEmail())
                .company(data.getCompany())
                .position(data.getPosition())
                .address(data.getAddress())
                .telephone(data.getTelephone())
                .website(data.getWebsite())
                .rawFields(rawFields)
                .build();
    }

    /**
     * 构建 rawFields：将 OCR 原始文本按字段分类，供前端展示置信度。
     */
    private Map<String, String> buildRawFields(BusinessCardData data) {
        Map<String, String> map = new LinkedHashMap<>();
        putIfNotBlank(map, "name", data.getName());
        putIfNotBlank(map, "phone", data.getMobile());
        putIfNotBlank(map, "email", data.getEmail());
        putIfNotBlank(map, "company", data.getCompany());
        putIfNotBlank(map, "position", data.getPosition());
        putIfNotBlank(map, "address", data.getAddress());
        putIfNotBlank(map, "telephone", data.getTelephone());
        putIfNotBlank(map, "website", data.getWebsite());
        putIfNotBlank(map, "rawText", data.getRawText());
        return map;
    }

    private void putIfNotBlank(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}
