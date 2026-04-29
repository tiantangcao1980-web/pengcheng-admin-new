package com.pengcheng.integration.spi.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 通讯录同步结果汇总。
 */
@Data
@Accessors(chain = true)
public class ContactSyncResult {

    /** 同步部门数量 */
    private int deptSynced;

    /** 同步用户数量 */
    private int userSynced;

    /** 新增用户数 */
    private int userCreated;

    /** 更新用户数 */
    private int userUpdated;

    /** 耗时（ms） */
    private long durationMs;

    /** 是否成功 */
    private boolean success;

    /** 错误信息（失败时填充） */
    private String errorMsg;
}
