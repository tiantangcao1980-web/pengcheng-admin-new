package com.pengcheng.system.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** OpenAPI 调用日志（V71）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("openapi_call_log")
public class OpenapiCallLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String accessKey;
    private Long tenantId;
    private String method;
    private String path;
    private Integer statusCode;
    private String requestId;
    private Integer durationMs;
    private String requestIp;
    private String errorMsg;
    private LocalDateTime createTime;
}
