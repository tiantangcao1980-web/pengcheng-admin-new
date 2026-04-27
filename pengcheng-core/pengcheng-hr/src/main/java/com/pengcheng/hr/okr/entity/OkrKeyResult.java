package com.pengcheng.hr.okr.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OKR 关键结果（Key Result）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("okr_key_result")
public class OkrKeyResult {

    /** 度量类型 */
    public static final String MEASURE_NUMBER = "NUMBER";
    public static final String MEASURE_PERCENT = "PERCENT";
    public static final String MEASURE_MILESTONE = "MILESTONE";
    public static final String MEASURE_BOOLEAN = "BOOLEAN";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long objectiveId;

    private String title;

    /** NUMBER/PERCENT/MILESTONE/BOOLEAN */
    private String measureType;

    private BigDecimal targetValue;

    private BigDecimal currentValue;

    private String unit;

    /** 进度 0-100 */
    private Integer progress;

    /** 权重，默认 25，多个 KR 权重之和不强制 100 但建议平衡 */
    private Integer weight;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
