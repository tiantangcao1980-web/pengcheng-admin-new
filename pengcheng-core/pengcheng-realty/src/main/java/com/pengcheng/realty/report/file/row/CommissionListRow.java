package com.pengcheng.realty.report.file.row;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ColumnWidth(16)
public class CommissionListRow {

    @ExcelProperty("佣金ID")
    private Long id;

    @ExcelProperty("成交ID")
    private Long dealId;

    @ExcelProperty("项目ID")
    private Long projectId;

    @ExcelProperty("应收金额")
    @ColumnWidth(15)
    private BigDecimal receivableAmount;

    @ExcelProperty("应结金额")
    @ColumnWidth(15)
    private BigDecimal payableAmount;

    @ExcelProperty("平台费")
    private BigDecimal platformFee;

    @ExcelProperty("审批节点")
    @ColumnWidth(22)
    private String approvalNode;

    @ExcelProperty("创建时间")
    @ColumnWidth(20)
    private String createTime;
}
