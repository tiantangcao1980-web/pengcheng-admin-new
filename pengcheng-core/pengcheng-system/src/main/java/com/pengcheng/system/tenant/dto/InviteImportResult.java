package com.pengcheng.system.tenant.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入邀请结果
 */
@Data
public class InviteImportResult {

    private int totalCount;
    private int successCount;
    private int failCount;
    private final List<InviteImportRow> rows = new ArrayList<>();

    public void add(InviteImportRow row) {
        rows.add(row);
        totalCount++;
        if (Boolean.TRUE.equals(row.getSuccess())) {
            successCount++;
        } else {
            failCount++;
        }
    }
}
