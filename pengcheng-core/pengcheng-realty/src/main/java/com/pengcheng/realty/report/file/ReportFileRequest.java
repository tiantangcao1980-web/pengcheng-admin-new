package com.pengcheng.realty.report.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * 报表生成请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFileRequest {

    /** 报表类型 */
    private ReportFileType type;

    /** 起始日期（按需） */
    private LocalDate startDate;

    /** 结束日期（按需） */
    private LocalDate endDate;

    /** 单客户报表用的客户ID */
    private Long customerId;

    /** 自定义参数（项目筛选 / 业务员筛选等） */
    private Map<String, Object> extraParams;
}
