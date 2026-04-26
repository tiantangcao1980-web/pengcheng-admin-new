package com.pengcheng.crm.tag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("customer_tag_rel")
public class CustomerTagRel implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;
    private Long tagId;
    private Long tenantId;
    private Long createBy;
    private LocalDateTime createTime;
}
