package com.pengcheng.hr.okr.dto;

import lombok.Data;

/**
 * 创建目标请求 DTO
 */
@Data
public class CreateObjectiveDTO {

    private Long periodId;

    private Long ownerId;

    /** USER/DEPT/COMPANY */
    private String ownerType;

    /** 上级目标 id，顶级传 null */
    private Long parentId;

    private String title;

    private String description;

    /** 权重，默认 100 */
    private Integer weight;
}
