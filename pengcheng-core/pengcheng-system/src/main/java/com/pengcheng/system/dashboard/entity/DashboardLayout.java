package com.pengcheng.system.dashboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户/角色看板布局实体（对应 dashboard_layout 表）
 *
 * <p>{@code layoutJson} 存储 JSON 数组，格式示例：
 * {@code [{"cardCode":"sales.funnel","x":0,"y":0,"w":4,"h":3}, ...]}
 */
@Data
@TableName("dashboard_layout")
public class DashboardLayout implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属类型：USER / ROLE */
    private String ownerType;

    /** 归属 ID（用户 ID 或角色 ID）*/
    private Long ownerId;

    /** 布局名称 */
    private String name;

    /** 布局 JSON（网格位置列表）*/
    private String layoutJson;

    /** 是否默认布局：1 是，0 否 */
    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
