package com.pengcheng.realty.unit.dto;

import lombok.Data;

/**
 * 房源锁定请求
 */
@Data
public class LockRequest {

    /** 锁定人 ID */
    private Long userId;

    /** 锁定时长（小时），默认 2 小时 */
    private Integer hours;
}
