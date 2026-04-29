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
public class CustomerAnalysisRow {

    @ExcelProperty("客户ID")
    private Long id;

    @ExcelProperty("客户姓名")
    @ColumnWidth(18)
    private String customerName;

    @ExcelProperty("脱敏手机号")
    @ColumnWidth(16)
    private String phoneMasked;

    @ExcelProperty("到访次数")
    private Integer visitCount;

    @ExcelProperty("成交概率")
    private BigDecimal dealProbability;

    @ExcelProperty("当前状态")
    private String statusLabel;

    @ExcelProperty("最近跟进时间")
    @ColumnWidth(20)
    private String lastFollowTime;
}
