package com.pengcheng.realty.report.file.generator;

import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.report.file.ReportFileRequest;
import com.pengcheng.realty.report.file.ReportFileResult;
import com.pengcheng.realty.report.file.ReportFileType;
import com.pengcheng.realty.report.file.row.CustomerAnalysisRow;
import com.pengcheng.realty.report.file.util.ExcelWriteHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RealtyCustomerMapper customerMapper;

    @Override
    public ReportFileType supportedType() {
        return ReportFileType.CUSTOMER_ANALYSIS;
    }

    @Override
    public ReportFileResult generate(ReportFileRequest req) {
        List<Customer> customers = customerMapper.selectList(null);

        List<CustomerAnalysisRow> rows = customers.stream()
                .map(c -> CustomerAnalysisRow.builder()
                        .id(c.getId())
                        .customerName(c.getCustomerName())
                        .phoneMasked(c.getPhoneMasked())
                        .visitCount(c.getVisitCount())
                        .dealProbability(c.getDealProbability())
                        .statusLabel(translateStatus(c.getStatus()))
                        .lastFollowTime(c.getLastFollowTime() == null ? "" : c.getLastFollowTime().format(FMT))
                        .build())
                .toList();

        byte[] bytes = ExcelWriteHelper.writeBytes(CustomerAnalysisRow.class, "客户分析", rows);
        String fileName = String.format("客户分析-%s.xlsx", LocalDate.now());
        log.info("[报表] 客户分析 行数={} 文件={}", rows.size(), fileName);
        return ExcelWriteHelper.buildExcelResult(fileName, bytes);
    }

    private String translateStatus(Integer s) {
        if (s == null) {
            return "未知";
        }
        return switch (s) {
            case 1 -> "新客户";
            case 2 -> "跟进中";
            case 3 -> "已成交";
            case 4 -> "已流失";
            default -> "其他";
        };
    }
}
