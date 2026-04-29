package com.pengcheng.system.dashboard.spi;

import java.util.Map;

/**
 * 看板卡片 SPI（Phase 3 数据决策）。
 *
 * <p>每张预置/自定义卡片都是一个 Spring {@code @Component}，由 {@code DashboardCardRegistry} 自动收集。
 * 卡片只暴露：①唯一标识 ②元数据（名字/分类/适用角色）③根据上下文计算业务数据。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * @Component
 * public class SalesFunnelCardProvider implements DashboardCardProvider {
 *     public String code()         { return "sales.funnel"; }
 *     public DashboardCardMetadata metadata() { ... }
 *     public Object render(DashboardCardContext ctx) { return ...; }
 * }
 * }</pre>
 *
 * <p>前端通过 {@code GET /admin/dashboard/cards} 拿到所有 metadata，
 * 通过 {@code POST /admin/dashboard/cards/{code}/render} 拉取该卡片在当前用户上下文下的具体数据。
 *
 * <p>SPI 设计原则：
 * <ul>
 *   <li><b>无状态</b>：实现类不持有用户/请求级状态，所有上下文从 {@link DashboardCardContext} 取；</li>
 *   <li><b>租户感知</b>：实现内部要尊重 {@code DataPermissionInterceptor} + {@code @DataScope}；</li>
 *   <li><b>错误隔离</b>：抛出的异常由 {@code DashboardCardRegistry} 兜底捕获并降级为"卡片不可用"占位，不传染整个看板。</li>
 * </ul>
 */
public interface DashboardCardProvider {

    /** 卡片唯一编码，建议格式 "{domain}.{name}"，例如 "sales.funnel"、"customer.health"。 */
    String code();

    /** 卡片静态元数据（名字、分类、适用角色、默认尺寸等）。 */
    DashboardCardMetadata metadata();

    /**
     * 根据当前用户/角色/时间范围等上下文渲染卡片数据。
     *
     * @param context 调用上下文（当前用户、租户、时间窗口、附加参数）
     * @return 卡片业务数据（结构由前端图表组件约定，建议 Map/POJO 而非裸字符串）
     */
    Object render(DashboardCardContext context);

    /** 渲染上下文。 */
    interface DashboardCardContext {
        Long userId();
        Long tenantId();
        java.time.LocalDateTime windowStart();
        java.time.LocalDateTime windowEnd();
        Map<String, Object> params();
    }

    /** 卡片元数据。 */
    interface DashboardCardMetadata {
        /** 显示名（中文，例如 "销售漏斗"）。 */
        String name();
        /** 业务分类（"sales" / "customer" / "team" / "finance" 等）。 */
        String category();
        /** 适用角色 codes（"admin"/"manager"/"sales" 等），空集表示所有角色可见。 */
        java.util.Set<String> applicableRoles();
        /** 默认网格尺寸 (cols, rows)，1 ≤ cols ≤ 12，1 ≤ rows ≤ 10。 */
        int defaultCols();
        int defaultRows();
        /** 推荐图表类型（"line"/"bar"/"pie"/"funnel"/"table"/"number" 等）。 */
        String suggestedChart();
        /** 简短描述。 */
        String description();
    }
}
