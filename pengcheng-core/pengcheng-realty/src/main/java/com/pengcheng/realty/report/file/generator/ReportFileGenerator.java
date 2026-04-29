package com.pengcheng.realty.report.file.generator;

import com.pengcheng.realty.report.file.ReportFileRequest;
import com.pengcheng.realty.report.file.ReportFileResult;
import com.pengcheng.realty.report.file.ReportFileType;

/**
 * 报表文件生成器接口（每种报表类型一个实现）
 */
public interface ReportFileGenerator {

    /** 支持的报表类型 */
    ReportFileType supportedType();

    /** 生成报表文件 */
    ReportFileResult generate(ReportFileRequest request);
}
