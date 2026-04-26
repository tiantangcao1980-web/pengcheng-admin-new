package com.pengcheng.crm.importexport;

import com.pengcheng.crm.importexport.dto.CustomerImportRowDTO;
import com.pengcheng.crm.importexport.dto.ImportResultVO;
import com.pengcheng.crm.importexport.service.CustomerImportExportService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证模板/失败行导出能成功写出非空字节流。
 */
class CustomerImportExportServiceTemplateTest {

    @Test
    void exportTemplate_writes_xlsx_bytes() {
        CustomerImportExportService svc = new CustomerImportExportService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        svc.exportTemplate(out);
        assertTrue(out.size() > 100, "模板字节流应非空");
    }

    @Test
    void exportFailedRows_writes_xlsx_bytes() {
        CustomerImportExportService svc = new CustomerImportExportService();
        ImportResultVO result = new ImportResultVO();
        CustomerImportRowDTO row = new CustomerImportRowDTO();
        row.setName("张三");
        row.setPhone("abc");
        result.addFail(2, "手机号格式非法", row);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        svc.exportFailedRows(out, result);
        assertTrue(out.size() > 100);
    }

    @Test
    void exportFailedRows_handles_empty_list() {
        CustomerImportExportService svc = new CustomerImportExportService();
        ImportResultVO result = new ImportResultVO();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        svc.exportFailedRows(out, result);
        assertTrue(out.size() > 0);
    }

    @Test
    void dryRun_with_null_or_empty_returns_empty_result() {
        ImportResultVO r = CustomerImportExportService.dryRun(null);
        assertTrue(r.getTotal() == 0);
        ImportResultVO r2 = CustomerImportExportService.dryRun(Collections.emptyList());
        assertTrue(r2.getTotal() == 0);
    }
}
