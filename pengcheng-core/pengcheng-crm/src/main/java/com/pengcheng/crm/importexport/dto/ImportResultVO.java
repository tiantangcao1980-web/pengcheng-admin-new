package com.pengcheng.crm.importexport.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果（含失败行反馈）
 */
@Data
public class ImportResultVO {

    /** 总行数（不含表头） */
    private int total;

    /** 成功导入行数 */
    private int success;

    /** 失败行数 */
    private int failed;

    /** 失败明细：行号 + 错误信息（行号从 2 起，因为 1 是表头） */
    private List<FailedRow> failedRows = new ArrayList<>();

    @Data
    public static class FailedRow {
        private int rowNum;
        private String message;
        private CustomerImportRowDTO row;

        public static FailedRow of(int rowNum, String message, CustomerImportRowDTO row) {
            FailedRow f = new FailedRow();
            f.setRowNum(rowNum);
            f.setMessage(message);
            f.setRow(row);
            return f;
        }
    }

    public void addFail(int rowNum, String message, CustomerImportRowDTO row) {
        this.failedRows.add(FailedRow.of(rowNum, message, row));
        this.failed++;
    }

    public void incrSuccess() {
        this.success++;
    }
}
