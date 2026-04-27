package com.pengcheng.system.ocr.integration;

import com.pengcheng.system.ocr.BusinessCardData;
import com.pengcheng.system.ocr.BusinessCardOcrService;
import com.pengcheng.system.ocr.baidu.OcrCallException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link CardImportToCustomerService} 单元测试
 *
 * <p>使用 Mockito 模拟 {@link BusinessCardOcrService}，聚焦字段映射逻辑和异常包装。</p>
 */
class CardImportToCustomerServiceImplTest {

    private BusinessCardOcrService mockOcrService;
    private CardImportToCustomerService service;

    /** 合法的 1x1 像素 Base64 JPEG（用于绕过解码校验） */
    private static final String VALID_BASE64 = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4});

    @BeforeEach
    void setUp() {
        mockOcrService = mock(BusinessCardOcrService.class);
        service = new CardImportToCustomerService(mockOcrService);
    }

    // ------------------------------------------------------------------ //
    //  用例 1：完整识别 → name/phone/email/company/position 全填            //
    // ------------------------------------------------------------------ //

    @Test
    void preview_fullCard_allFieldsMapped() {
        BusinessCardData fullData = BusinessCardData.builder()
                .name("李雷")
                .mobile("13812345678")
                .email("leil@example.com")
                .company("腾讯科技（深圳）有限公司")
                .position("高级工程师")
                .address("广东省深圳市南山区科技园路1号")
                .telephone("0755-86013388")
                .website("https://www.example.com")
                .rawText("李雷\n腾讯科技（深圳）有限公司\n高级工程师")
                .build();
        when(mockOcrService.recognize(any())).thenReturn(fullData);

        CustomerImportPreview preview = service.previewFromImage(VALID_BASE64);

        assertEquals("李雷", preview.getName());
        assertEquals("13812345678", preview.getPhone());
        assertEquals("leil@example.com", preview.getEmail());
        assertTrue(preview.getCompany().contains("腾讯科技"));
        assertEquals("高级工程师", preview.getPosition());
        assertNotNull(preview.getAddress());
        assertEquals("0755-86013388", preview.getTelephone());
        assertEquals("https://www.example.com", preview.getWebsite());
        assertNotNull(preview.getRawFields());
        assertTrue(preview.getRawFields().containsKey("name"));
    }

    // ------------------------------------------------------------------ //
    //  用例 2：部分缺失（仅 phone + name）→ 其它字段 null                  //
    // ------------------------------------------------------------------ //

    @Test
    void preview_partialCard_missingFieldsAreNull() {
        BusinessCardData partial = BusinessCardData.builder()
                .name("张三")
                .mobile("13900001111")
                .build();
        when(mockOcrService.recognize(any())).thenReturn(partial);

        CustomerImportPreview preview = service.previewFromImage(VALID_BASE64);

        assertEquals("张三", preview.getName());
        assertEquals("13900001111", preview.getPhone());
        assertNull(preview.getEmail());
        assertNull(preview.getCompany());
        assertNull(preview.getPosition());
        assertNull(preview.getAddress());
    }

    // ------------------------------------------------------------------ //
    //  用例 3：多电话/多邮箱 → 取第一条                                      //
    // ------------------------------------------------------------------ //

    @Test
    void preview_multiplePhoneAndEmail_firstOneSelected() {
        // BusinessCardParser 在 OCR SPI 层已取第一条 mobile/email；
        // 本测试验证 Service 原样透传 BusinessCardData.mobile 和 email，
        // 不会被覆盖或拼接。
        BusinessCardData data = BusinessCardData.builder()
                .mobile("13800000001")   // 已由 Parser 取第一条
                .email("first@example.com")
                .build();
        when(mockOcrService.recognize(any())).thenReturn(data);

        CustomerImportPreview preview = service.previewFromImage(VALID_BASE64);

        assertEquals("13800000001", preview.getPhone());
        assertEquals("first@example.com", preview.getEmail());
    }

    // ------------------------------------------------------------------ //
    //  用例 4：BusinessCardOcrService 抛 OcrCallException → CardPreviewException //
    // ------------------------------------------------------------------ //

    @Test
    void preview_ocrCallException_wrappedAsCardPreviewException() {
        when(mockOcrService.recognize(any()))
                .thenThrow(new OcrCallException("百度 OCR 业务错误 error_code=216202: image not found", 216202));

        CardPreviewException ex = assertThrows(
                CardPreviewException.class,
                () -> service.previewFromImage(VALID_BASE64));

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("名片识别失败"));
        assertInstanceOf(OcrCallException.class, ex.getCause());
    }

    // ------------------------------------------------------------------ //
    //  用例 5：空图片入参 → IllegalArgumentException                        //
    // ------------------------------------------------------------------ //

    @Test
    void preview_nullOrBlankBase64_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.previewFromImage(null));

        assertThrows(IllegalArgumentException.class,
                () -> service.previewFromImage(""));

        assertThrows(IllegalArgumentException.class,
                () -> service.previewFromImage("   "));

        // OcrService 不应被调用
        verifyNoInteractions(mockOcrService);
    }
}
