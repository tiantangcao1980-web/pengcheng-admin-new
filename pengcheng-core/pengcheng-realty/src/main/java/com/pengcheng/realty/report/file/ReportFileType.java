package com.pengcheng.realty.report.file;

import lombok.Getter;

/**
 * 报表文件类型
 */
@Getter
public enum ReportFileType {

    /** 销售业绩 Excel：按业务员/时间段汇总成交数与金额 */
    SALES_PERFORMANCE("销售业绩", "xlsx", "sales-performance"),

    /** 客户分析 Excel：当前客户列表 + 阶段统计 */
    CUSTOMER_ANALYSIS("客户分析", "xlsx", "customer-analysis"),

    /** 佣金清单 Excel：佣金记录明细 + 审批状态 */
    COMMISSION_LIST("佣金清单", "xlsx", "commission-list"),

    /** 客户跟进报告 Word：单客户全流程纪要 */
    CUSTOMER_FOLLOWUP_REPORT("客户跟进报告", "docx", "customer-followup-report");

    private final String displayName;
    private final String extension;
    private final String code;

    ReportFileType(String displayName, String extension, String code) {
        this.displayName = displayName;
        this.extension = extension;
        this.code = code;
    }

    public static ReportFileType fromCode(String code) {
        for (ReportFileType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        throw new IllegalArgumentException("未知报表类型: " + code);
    }
}
