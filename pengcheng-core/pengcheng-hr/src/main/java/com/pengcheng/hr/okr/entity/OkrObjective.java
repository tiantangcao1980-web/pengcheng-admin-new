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

import java.time.LocalDateTime;

/**
 * OKR 目标（Objective）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("okr_objective")
public class OkrObjective {

    /** ownerType 常量 */
    public static final String OWNER_USER = "USER";
    public static final String OWNER_DEPT = "DEPT";
    public static final String OWNER_COMPANY = "COMPANY";

    /** 状态：0进行中 1完成 2取消 */
    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_DONE = 1;
    public static final int STATUS_CANCELLED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long periodId;

    private Long ownerId;

    private String ownerType;

    /** 上级目标（对齐链），顶级为 null */
    private Long parentId;

    private String title;

    private String description;

    /** 总进度 0-100，由所有 KR 加权平均自动算 */
    private Integer progress;

    private Integer weight;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
