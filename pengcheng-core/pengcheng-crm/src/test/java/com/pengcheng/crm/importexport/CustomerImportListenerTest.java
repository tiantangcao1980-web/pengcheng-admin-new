package com.pengcheng.crm.importexport;

import com.pengcheng.crm.importexport.dto.CustomerImportRowDTO;
import com.pengcheng.crm.importexport.dto.ImportResultVO;
import com.pengcheng.crm.importexport.service.CustomerImportExportService;
import com.pengcheng.crm.importexport.service.CustomerImportListener;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerImportListenerTest {

    private CustomerImportRowDTO row(String name, String phone, String email) {
        CustomerImportRowDTO r = new CustomerImportRowDTO();
        r.setName(name);
        r.setPhone(phone);
        r.setEmail(email);
        return r;
    }

    @Test
    void validate_blank_name_fails() {
        ImportResultVO result = new ImportResultVO();
        CustomerImportListener listener = new CustomerImportListener(result, (r, res) -> {});
        assertNotNull(listener.validate(row(null, "13800001234", null)));
        assertNotNull(listener.validate(row("", "13800001234", null)));
    }

    @Test
    void validate_invalid_phone_fails() {
        ImportResultVO result = new ImportResultVO();
        CustomerImportListener listener = new CustomerImportListener(result, (r, res) -> {});
        assertNotNull(listener.validate(row("张三", null, null)));
        assertNotNull(listener.validate(row("张三", "abc", null)));
        assertNotNull(listener.validate(row("张三", "12345678901", null)));
    }

    @Test
    void validate_phone_dedup_within_batch() {
        ImportResultVO result = new ImportResultVO();
        CustomerImportListener listener = new CustomerImportListener(result, (r, res) -> {});
        assertNull(listener.validate(row("A", "13800001234", null)));
        // 第二次同号
        String err = listener.validate(row("B", "13800001234", null));
        assertNotNull(err);
        assertTrue(err.contains("重复"));
    }

    @Test
    void validate_intention_must_be_high_mid_low() {
        ImportResultVO result = new ImportResultVO();
        CustomerImportListener listener = new CustomerImportListener(result, (r, res) -> {});
        CustomerImportRowDTO r = row("张三", "13899990000", null);
        r.setIntentionLevel("非常高");
        assertNotNull(listener.validate(r));
        r.setPhone("13899990001");
        r.setIntentionLevel("高");
        assertNull(listener.validate(r));
    }

    @Test
    void dryRun_records_failed_rows_with_correct_rowNum() {
        CustomerImportRowDTO good = row("张三", "13800000001", null);
        CustomerImportRowDTO badPhone = row("李四", "abc", null);
        CustomerImportRowDTO badName = row(null, "13800000003", null);
        List<CustomerImportRowDTO> rows = Arrays.asList(good, badPhone, badName);

        ImportResultVO result = CustomerImportExportService.dryRun(rows);
        assertEquals(3, result.getTotal());
        assertEquals(1, result.getSuccess());
        assertEquals(2, result.getFailed());
        // 表头是第 1 行；good=2, badPhone=3, badName=4
        assertEquals(3, result.getFailedRows().get(0).getRowNum());
        assertEquals(4, result.getFailedRows().get(1).getRowNum());
        // 失败行回写：原行内容仍可访问
        assertEquals("abc", result.getFailedRows().get(0).getRow().getPhone());
    }

    @Test
    void parseIntention_maps_string_to_int() {
        assertEquals(Integer.valueOf(1), CustomerImportExportService.parseIntention("高"));
        assertEquals(Integer.valueOf(2), CustomerImportExportService.parseIntention("中"));
        assertEquals(Integer.valueOf(3), CustomerImportExportService.parseIntention("低"));
        assertEquals(Integer.valueOf(2), CustomerImportExportService.parseIntention(null));
        assertEquals(Integer.valueOf(2), CustomerImportExportService.parseIntention("xxx"));
    }

    @Test
    void supportedColumns_lists_all_template_headers() {
        List<String> cols = CustomerImportExportService.supportedColumns();
        assertTrue(cols.contains("姓名"));
        assertTrue(cols.contains("手机号"));
        assertTrue(cols.contains("意向(高/中/低)"));
    }
}
