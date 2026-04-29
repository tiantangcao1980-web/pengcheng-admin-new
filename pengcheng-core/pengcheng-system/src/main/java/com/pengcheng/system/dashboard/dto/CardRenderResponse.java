package com.pengcheng.system.dashboard.dto;

import com.pengcheng.system.dashboard.entity.DashboardCardDef;
import lombok.Data;

/**
 * 看板卡片渲染响应
 *
 * <p>同时携带卡片 metadata（供前端渲染图表类型/尺寸）和业务数据。
 * 当卡片 provider 抛出异常时，{@code data} 为 {@code {"error": "...message..."}}，
 * {@code success} 为 {@code false}，前端降级展示"卡片暂不可用"。
 */
@Data
public class CardRenderResponse {

    /** 卡片定义元信息 */
    private DashboardCardDef meta;

    /** 卡片业务数据（正常时为 provider 返回值，异常时为 error map）*/
    private Object data;

    /** 渲染是否成功 */
    private boolean success;

    public static CardRenderResponse ok(DashboardCardDef meta, Object data) {
        CardRenderResponse r = new CardRenderResponse();
        r.meta = meta;
        r.data = data;
        r.success = true;
        return r;
    }

    public static CardRenderResponse error(DashboardCardDef meta, String message) {
        CardRenderResponse r = new CardRenderResponse();
        r.meta = meta;
        r.data = java.util.Map.of("error", message);
        r.success = false;
        return r;
    }
}
