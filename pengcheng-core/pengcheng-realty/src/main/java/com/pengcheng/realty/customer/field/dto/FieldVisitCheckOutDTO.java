package com.pengcheng.realty.customer.field.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldVisitCheckOutDTO {

    private Long fieldVisitId;

    private Long userId;

    /** 拜访结果 */
    private String result;

    /** 签退时补充的照片 */
    private String additionalPhotoUrls;
}
