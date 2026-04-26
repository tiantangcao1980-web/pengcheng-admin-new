package com.pengcheng.crm.lead.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LeadCreateDTO {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 32)
    private String phone;

    @Size(max = 120)
    private String email;

    @Size(max = 64)
    private String wechat;

    @Size(max = 200)
    private String company;

    private String source;
    private String sourceDetail;

    /** 1-高 2-中 3-低 */
    private Integer intentionLevel;

    private Long ownerId;
    private Long deptId;

    @Size(max = 1000)
    private String remark;
}
