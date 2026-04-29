package com.pengcheng.realty.report.file.util;

import com.alibaba.excel.EasyExcel;
import com.pengcheng.realty.report.file.ReportFileResult;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Excel 报表写出工具
 *
 * 统一封装 EasyExcel 写入 + ReportFileResult 构建逻辑，避免在各 Generator 中重复。
 */
@Slf4j
public final class ExcelWriteHelper {

    /** Excel (xlsx) 标准 MIME */
    public static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private ExcelWriteHelper() {
    }

    /**
     * 将 rows 写入单 sheet 的 xlsx 字节流。
     *
     * @param clazz     行模型 Class（EasyExcel 通过注解解析表头）
     * @param sheetName Sheet 名
     * @param rows      数据行
     * @return xlsx 字节内容
     */
    public static <T> byte[] writeBytes(Class<T> clazz, String sheetName, List<T> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            EasyExcel.write(out, clazz).sheet(sheetName).doWrite(rows);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[报表] Excel 写入失败 sheet={} rows={}", sheetName, rows == null ? 0 : rows.size(), e);
            throw new RuntimeException("Excel 报表生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将字节流包装为 xlsx 类型的 ReportFileResult。
     */
    public static ReportFileResult buildExcelResult(String fileName, byte[] bytes) {
        return ReportFileResult.builder()
                .fileName(fileName)
                .contentType(XLSX_CONTENT_TYPE)
                .content(bytes)
                .size(bytes.length)
                .build();
    }
}
