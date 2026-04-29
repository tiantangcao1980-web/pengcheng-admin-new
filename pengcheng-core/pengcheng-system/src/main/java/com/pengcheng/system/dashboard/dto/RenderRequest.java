package com.pengcheng.system.dashboard.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 看板卡片渲染请求
 */
@Data
public class RenderRequest {

    /** 时间窗口开始（可为 null，由卡片自行设默认）*/
    private LocalDateTime windowStart;

    /** 时间窗口结束 */
    private LocalDateTime windowEnd;

    /** 附加参数（过滤条件、维度等）*/
    private Map<String, Object> params;
}
