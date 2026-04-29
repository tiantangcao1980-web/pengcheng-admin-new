package com.pengcheng.system.dashboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 看板卡片定义实体（对应 dashboard_card_def 表）
 *
 * <p>数据来源：启动时由 {@code DashboardCardRegistry#syncToDb()} 从 SPI 元数据同步写入；
 * 后续可在管理界面手动启用/禁用（enabled 字段）。
 */
@Data
@TableName("dashboard_card_def")
public class DashboardCardDef implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 卡片唯一编码，对齐 {@code DashboardCardProvider#code()} */
    private String code;

    /** 显示名称 */
    private String name;

    /** 业务分类 */
    private String category;

    /** 推荐图表类型 */
    private String suggestedChart;

    /** 默认列宽（网格列数）*/
    private Integer defaultCols;

    /** 默认行高（网格行数）*/
    private Integer defaultRows;

    /** 简短描述 */
    private String description;

    /** 是否启用：1 启用，0 禁用 */
    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
