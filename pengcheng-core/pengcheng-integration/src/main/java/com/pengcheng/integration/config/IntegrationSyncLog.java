package com.pengcheng.integration.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 同步任务日志实体（对应 integration_sync_log）。
 */
@Data
@TableName("integration_sync_log")
public class IntegrationSyncLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** provider 标识 */
    private String provider;

    /** 同步类型：CONTACT / MESSAGE / APPROVAL */
    private String syncType;

    /** 是否成功（1=成功，0=失败） */
    private Integer success;

    /** 影响条数 */
    private Integer affected;

    /** 错误信息（失败时填充） */
    private String errorMsg;

    /** 耗时（ms） */
    private Integer durationMs;

    private LocalDateTime createTime;
}
