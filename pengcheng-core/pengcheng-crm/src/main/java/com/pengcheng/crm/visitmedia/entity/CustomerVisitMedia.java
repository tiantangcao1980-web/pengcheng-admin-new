package com.pengcheng.crm.visitmedia.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 与 realty 的 customer_visit 物理表共用，仅暴露多媒体相关字段。
 * <p>红线：不修改 realty.CustomerVisit 实体，不改业务逻辑；
 * 该实体只用于多媒体跟进 Service 的读写。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "customer_visit", autoResultMap = true)
public class CustomerVisitMedia implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    /** text/image/audio/video/mixed */
    private String mediaType;

    /** JSON 字符串：MinIO key 数组 */
    private String mediaUrls;

    /** 语音时长（秒） */
    private Integer voiceDuration;

    private String remark;

    private LocalDateTime updateTime;
}
