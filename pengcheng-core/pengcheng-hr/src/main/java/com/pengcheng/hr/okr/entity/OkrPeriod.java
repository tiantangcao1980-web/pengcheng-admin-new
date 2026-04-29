package com.pengcheng.hr.okr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * OKR 周期（季度/年度）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("okr_period")
public class OkrPeriod {

    /** 状态：0草稿 1进行中 2已结束 */
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_CLOSED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 周期编码，如 2026Q2 */
    private String code;

    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer status;

    private LocalDateTime createTime;
}
