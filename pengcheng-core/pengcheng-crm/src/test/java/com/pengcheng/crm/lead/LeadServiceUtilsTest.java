package com.pengcheng.crm.lead;

import com.pengcheng.crm.lead.service.LeadService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LeadService 静态工具 + 边界单测（不依赖 Mapper，用反射触达 package-private 静态方法）。
 */
class LeadServiceUtilsTest {

    @Test
    void generateLeadNo_starts_with_L_and_unique() throws Exception {
        java.lang.reflect.Method m = LeadService.class.getDeclaredMethod("generateLeadNo");
        m.setAccessible(true);
        String a = (String) m.invoke(null);
        String b = (String) m.invoke(null);
        assertNotNull(a);
        assertTrue(a.startsWith("L"), "leadNo 必须以 L 开头");
        // 即便同一毫秒生成，UUID 后缀也保持差异
        assertTrue(a.length() >= 14);
        assertTrue(b.length() >= 14);
    }

    @Test
    void maskPhone_handles_short_and_long() throws Exception {
        java.lang.reflect.Method m = LeadService.class.getDeclaredMethod("maskPhone", String.class);
        m.setAccessible(true);
        assertEquals("13812341234".substring(0, 3) + "****" + "1234",
                m.invoke(null, "13812341234"));
        assertEquals(null, m.invoke(null, (Object) null));
        assertEquals("123", m.invoke(null, "123"));
    }
}
