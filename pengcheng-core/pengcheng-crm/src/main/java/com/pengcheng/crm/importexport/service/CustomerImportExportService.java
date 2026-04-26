package com.pengcheng.crm.importexport.service;

import com.alibaba.excel.EasyExcel;
import com.pengcheng.common.exception.BizErrorCode;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.crm.importexport.dto.CustomerImportRowDTO;
import com.pengcheng.crm.importexport.dto.ImportResultVO;
import com.pengcheng.crm.lead.dto.LeadCreateDTO;
import com.pengcheng.crm.lead.service.LeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 客户/线索 Excel 导入导出 Service（基于 EasyExcel）。
 */
@Service
public class CustomerImportExportService {

    @Autowired
    private LeadService leadService;

    /**
     * 导入：当前实现把 Excel 行作为线索批量入库（最贴 PRD：线索 → 客户主干）。
     */
    public ImportResultVO importLeads(InputStream input) {
        if (input == null) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "上传文件不能为空");
        }
        ImportResultVO result = new ImportResultVO();
        CustomerImportListener listener = new CustomerImportListener(result, row -> {
            try {
                LeadCreateDTO dto = new LeadCreateDTO();
                dto.setName(row.getName());
                dto.setPhone(row.getPhone());
                dto.setEmail(row.getEmail());
                dto.setCompany(row.getCompany());
                dto.setSource(row.getSource() == null ? "import" : row.getSource());
                dto.setRemark(row.getRemark());
                dto.setIntentionLevel(parseIntention(row.getIntentionLevel()));
                leadService.create(dto);
                return null;
            } catch (Exception e) {
                return e.getMessage();
            }
        });
        EasyExcel.read(input, CustomerImportRowDTO.class, listener).sheet().doRead();
        return result;
    }

    /**
     * 导出模板（仅表头）
     */
    public void exportTemplate(OutputStream output) {
        EasyExcel.write(output, CustomerImportRowDTO.class)
                .sheet("线索模板")
                .doWrite(new ArrayList<CustomerImportRowDTO>());
    }

    /**
     * 导出失败行（用于失败行反馈）
     */
    public void exportFailedRows(OutputStream output, ImportResultVO result) {
        List<CustomerImportRowDTO> rows = new ArrayList<>();
        for (ImportResultVO.FailedRow fr : result.getFailedRows()) {
            CustomerImportRowDTO row = fr.getRow();
            if (row == null) continue;
            // 把错误原因塞到备注列
            row.setRemark("[行" + fr.getRowNum() + " 错误] " + fr.getMessage()
                    + (row.getRemark() == null ? "" : " | " + row.getRemark()));
            rows.add(row);
        }
        EasyExcel.write(output, CustomerImportRowDTO.class).sheet("失败行").doWrite(rows);
    }

    /** 仅供测试和上层调用：传入 CSV/Excel 行已解析的 List 进行导入 */
    public ImportResultVO importRows(List<CustomerImportRowDTO> rows) {
        ImportResultVO result = new ImportResultVO();
        CustomerImportListener listener = new CustomerImportListener(result, row -> {
            try {
                LeadCreateDTO dto = new LeadCreateDTO();
                dto.setName(row.getName());
                dto.setPhone(row.getPhone());
                dto.setEmail(row.getEmail());
                dto.setCompany(row.getCompany());
                dto.setSource(row.getSource() == null ? "import" : row.getSource());
                dto.setRemark(row.getRemark());
                dto.setIntentionLevel(parseIntention(row.getIntentionLevel()));
                leadService.create(dto);
                return null;
            } catch (Exception e) {
                return e.getMessage();
            }
        });
        for (CustomerImportRowDTO row : rows) {
            listener.invoke(row, null);
        }
        return result;
    }

    static Integer parseIntention(String s) {
        if (s == null || s.isBlank()) return 2;
        return switch (s.trim()) {
            case "高" -> 1;
            case "中" -> 2;
            case "低" -> 3;
            default -> 2;
        };
    }

    /** 测试便利：使用 Listener 但不实际落库（handler 返回 null 即成功） */
    public static ImportResultVO dryRun(List<CustomerImportRowDTO> rows) {
        ImportResultVO result = new ImportResultVO();
        CustomerImportListener listener = new CustomerImportListener(result, (row) -> null);
        if (rows != null) {
            for (CustomerImportRowDTO r : rows) {
                listener.invoke(r, null);
            }
        }
        return result;
    }

    public static List<String> supportedColumns() {
        return Arrays.asList("姓名", "手机号", "邮箱", "公司", "来源", "意向(高/中/低)", "备注");
    }
}
