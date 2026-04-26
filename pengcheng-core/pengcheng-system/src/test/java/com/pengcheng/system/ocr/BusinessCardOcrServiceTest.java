package com.pengcheng.system.ocr;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessCardOcrServiceTest {

    @Test
    void parser_extractsAllFields() {
        List<String> lines = List.of(
                "李雷",
                "腾讯科技（深圳）有限公司",
                "高级工程师",
                "Mobile: 138-1234-5678",
                "Tel: 0755-86013388",
                "Email: leil@example.com",
                "网址: https://www.example.com",
                "地址: 广东省深圳市南山区科技园路 1 号"
        );
        BusinessCardData data = BusinessCardParser.parse(lines);

        assertEquals("李雷", data.getName());
        assertTrue(data.getCompany().contains("腾讯科技"));
        assertTrue(data.getPosition().contains("工程师"));
        assertEquals("13812345678", data.getMobile());
        assertNotNull(data.getTelephone());
        assertEquals("leil@example.com", data.getEmail());
        assertNotNull(data.getWebsite());
        assertTrue(data.getAddress().contains("深圳"));
    }

    @Test
    void parser_handlesEnglishCard() {
        List<String> lines = List.of(
                "John Doe",
                "Acme Corp Ltd",
                "Director of Engineering",
                "+86-10-12345678",
                "13900001111",
                "john.doe@acme.com",
                "www.acme.com",
                "100 Main Street, Beijing"
        );
        BusinessCardData data = BusinessCardParser.parse(lines);
        assertEquals("13900001111", data.getMobile());
        assertEquals("john.doe@acme.com", data.getEmail());
        assertNotNull(data.getCompany());
        assertNotNull(data.getPosition());
    }

    @Test
    void parser_emptyOrNull_returnsBlank() {
        BusinessCardData a = BusinessCardParser.parse(null);
        BusinessCardData b = BusinessCardParser.parse(List.of());
        assertNull(a.getMobile());
        assertNull(b.getMobile());
    }

    @Test
    void parser_doesNotMistakeMobileAsTelephone() {
        List<String> lines = List.of("张伟", "13912345678");
        BusinessCardData data = BusinessCardParser.parse(lines);
        assertEquals("13912345678", data.getMobile());
        assertNull(data.getTelephone());
    }

    @Test
    void parser_skipsLinesWithDigitsForName() {
        List<String> lines = List.of("13812341234", "李雷", "经理");
        BusinessCardData data = BusinessCardParser.parse(lines);
        assertEquals("李雷", data.getName());
    }

    @Test
    void service_emptyImage_returnsBlank() {
        BusinessCardOcrService service = new BusinessCardOcrService(new StubProvider(List.of()));
        BusinessCardData data = service.recognize(null);
        assertNull(data.getName());
        BusinessCardData data2 = service.recognize(new byte[0]);
        assertNull(data2.getName());
    }

    @Test
    void service_noProvider_returnsBlank() {
        BusinessCardOcrService service = new BusinessCardOcrService(null);
        BusinessCardData data = service.recognize(new byte[]{1, 2, 3});
        assertNull(data.getName());
    }

    @Test
    void service_providerThrows_returnsBlank() {
        OcrProvider boom = new OcrProvider() {
            @Override public List<String> recognize(byte[] imageBytes) {
                throw new RuntimeException("API down");
            }
            @Override public String getProviderType() { return "stub"; }
        };
        BusinessCardOcrService service = new BusinessCardOcrService(boom);
        BusinessCardData data = service.recognize(new byte[]{1});
        assertNull(data.getName());
    }

    @Test
    void service_normalCall_parses() {
        BusinessCardOcrService service = new BusinessCardOcrService(
                new StubProvider(List.of("王五", "腾讯科技有限公司", "经理", "13800000000")));
        BusinessCardData data = service.recognize(new byte[]{1});
        assertEquals("王五", data.getName());
        assertEquals("13800000000", data.getMobile());
    }

    static class StubProvider implements OcrProvider {
        final List<String> result;
        StubProvider(List<String> result) { this.result = result; }
        @Override public List<String> recognize(byte[] imageBytes) { return result; }
        @Override public String getProviderType() { return "stub"; }
    }
}
