package com.pengcheng.realty.report.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 报表生成结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFileResult {

    /** 文件名（含扩展名，如 销售业绩-2026-04.xlsx） */
    private String fileName;

    /** 文件 MIME 类型 */
    private String contentType;

    /** 字节内容（小文件直接返回；大文件优先用 url） */
    private byte[] content;

    /** OSS 下载 URL（已上传时填） */
    private String url;

    /** 文件大小（字节） */
    private int size;
}
