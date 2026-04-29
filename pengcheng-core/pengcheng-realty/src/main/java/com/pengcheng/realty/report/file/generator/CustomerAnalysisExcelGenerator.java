package com.pengcheng.realty.report.file.generator;

import com.alibaba.excel.EasyExcel;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.report.file.ReportFileRequest;
import com.pengcheng.realty.report.file.ReportFileResult;
import com.pengcheng.realty.report.file.ReportFileType;
import com.pengcheng.realty.report.file.row.CustomerAnalysisRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 客户分析 Excel 生成器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerAnalysisExcelGenerator implements ReportFileGenerator {

    private final RealtyCustomerMapper customerMapper;

    @Override
    public ReportFileType supportedType() {
        return ReportFileType.CUSTOMER_ANALYSIS;
    }

    @Override
    public ReportFileResult generate(ReportFileRequest req) {
        List<Customer> customers = customerMapper.selectList(null);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        List<CustomerAnalysisRow> rows = customers.stream()
                .map(c -> CustomerAnalysisRow.builder()
                        .id(c.getId())
                        .customerName(c.getCustomerName())
                        .phoneMasked(c.getPhoneMasked())
                        .visitCount(c.getVisitCount())
                        .dealProbability(c.getDealProbability())
                        .statusLabel(translateStatus(c.getStatus()))
                        .lastFollowTime(c.getLastFollowTime() == null ? "" : c.getLastFollowTime().format(fmt))
                        .build())
                .toList();

        byte[] bytes = writeExcel(rows);
        String fileName = String.format("客户分析-%s.xlsx", LocalDate.now());
        log.info("[报表] 客户分析 行数={} 文件={}", rows.size(), fileName);
        return ReportFileResult.builder()
                .fileName(fileName)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .content(bytes)
                .size(bytes.length)
                .build();
    }

    private byte[] writeExcel(List<CustomerAnalysisRow> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            EasyExcel.write(out, CustomerAnalysisRow.class)
                    .sheet("客户分析").doWrite(rows);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[报表] 客户分析 写入失败", e);
            throw new RuntimeException("客户分析报表生成失败: " + e.getMessage(), e);
        }
    }

    private String translateStatus(Integer s) {
        if (s == null) return "未知";
        return switch (s) {
            case 1 -> "新客户";
            case 2 -> "跟进中";
            case 3 -> "已成交";
            case 4 -> "已流失";
            default -> "其他";
        };
    }
}
