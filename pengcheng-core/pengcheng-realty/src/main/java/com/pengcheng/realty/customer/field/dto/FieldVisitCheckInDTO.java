package com.pengcheng.realty.customer.field.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldVisitCheckInDTO {

    private Long userId;
    private Long customerId;
    private Long projectId;
    /** 1客户拜访 2楼盘踏勘 3带看 4其他；不填默认 1 */
    private Integer visitType;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    /** 拍照 URL 列表（逗号分隔） */
    private String photoUrls;
    private String purpose;
}
