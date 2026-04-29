package com.pengcheng.realty.report.file.row;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 销售业绩 Excel 行模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ColumnWidth(18)
public class SalesPerformanceRow {

    @ExcelProperty("成交日期")
    @ColumnWidth(20)
    private String dealTime;

    @ExcelProperty("客户ID")
    private Long customerId;

    @ExcelProperty("房号")
    private String roomNo;

    @ExcelProperty("成交金额")
    @ColumnWidth(15)
    private BigDecimal dealAmount;

    @ExcelProperty("签约状态")
    private String signStatus;

    @ExcelProperty("回款状态")
    private String paymentStatus;
}
