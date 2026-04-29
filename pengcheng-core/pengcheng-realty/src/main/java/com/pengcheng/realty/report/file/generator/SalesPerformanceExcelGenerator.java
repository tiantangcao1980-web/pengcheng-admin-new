package com.pengcheng.realty.report.file.generator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.report.file.ReportFileRequest;
import com.pengcheng.realty.report.file.ReportFileResult;
import com.pengcheng.realty.report.file.ReportFileType;
import com.pengcheng.realty.report.file.row.SalesPerformanceRow;
import com.pengcheng.realty.report.file.util.ExcelWriteHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 销售业绩 Excel 生成器
 *
 * 列：成交日期 | 客户ID | 房号 | 成交金额 | 签约状态 | 回款状态
 * 末行：合计金额
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesPerformanceExcelGenerator implements ReportFileGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CustomerDealMapper dealMapper;

    @Override
    public ReportFileType supportedType() {
        return ReportFileType.SALES_PERFORMANCE;
    }

    @Override
    public ReportFileResult generate(ReportFileRequest req) {
        LocalDate from = req.getStartDate() != null ? req.getStartDate() : LocalDate.now().minusMonths(1);
        LocalDate to = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();

        List<CustomerDeal> deals = dealMapper.selectList(new LambdaQueryWrapper<CustomerDeal>()
                .ge(CustomerDeal::getDealTime, from.atStartOfDay())
                .le(CustomerDeal::getDealTime, to.plusDays(1).atStartOfDay()));

        List<SalesPerformanceRow> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CustomerDeal d : deals) {
            BigDecimal amt = d.getDealAmount() == null ? BigDecimal.ZERO : d.getDealAmount();
            total = total.add(amt);
            rows.add(SalesPerformanceRow.builder()
                    .dealTime(d.getDealTime() == null ? "" : d.getDealTime().format(FMT))
                    .customerId(d.getCustomerId())
                    .roomNo(d.getRoomNo())
                    .dealAmount(amt)
                    .signStatus(translateSign(d.getSignStatus()))
                    .paymentStatus(translatePayment(d.getPaymentStatus()))
                    .build());
        }
        // 合计行
        rows.add(SalesPerformanceRow.builder()
                .dealTime("合计")
                .dealAmount(total)
                .build());

        byte[] bytes = ExcelWriteHelper.writeBytes(SalesPerformanceRow.class, "销售业绩", rows);
        String fileName = String.format("销售业绩-%s_%s.xlsx", from, to);
        log.info("[报表] 销售业绩 行数={} 总额={} 文件={}", deals.size(), total, fileName);
        return ExcelWriteHelper.buildExcelResult(fileName, bytes);
    }

    private String translateSign(Integer s) {
        if (s == null) {
            return "";
        }
        return switch (s) {
            case 1 -> "已签约";
            case 2 -> "退订";
            default -> "其他";
        };
    }

    private String translatePayment(Integer s) {
        if (s == null) {
            return "";
        }
        return switch (s) {
            case 1 -> "未付款";
            case 2 -> "部分付款";
            case 3 -> "已付清";
            default -> "其他";
        };
    }
}
