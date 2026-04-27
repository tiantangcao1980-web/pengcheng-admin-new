package com.pengcheng.realty.plugin;

import com.pengcheng.system.plugin.spi.IndustryPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 房产行业插件（Phase 5 首个参考实现）。
 *
 * <p>声明房产行业的菜单贡献、看板卡片 code 和自定义字段模板，
 * 由 {@link com.pengcheng.system.plugin.registry.IndustryPluginRegistry} 自动收集。
 *
 * <h3>菜单路径约定</h3>
 * 路径与 K2（楼盘/房源管理）、K3（带看 SOP）、K5（联盟商/佣金）预期路由对齐，
 * 前端页面由各 K 实现后挂载至对应 component 路径。
 *
 * <h3>看板卡片</h3>
 * 仅声明 code，具体卡片实现由 K4（房产专属看板）完成。
 *
 * <h3>字段模板</h3>
 * 复用 V4 自定义字段机制，为客户实体提供房产行业预置字段。
 */
@Slf4j
@Component
public class RealtyIndustryPlugin implements IndustryPlugin {

    @Override
    public String code() {
        return "realty";
    }

    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata() {
            @Override public String name()        { return "房产销售"; }
            @Override public String version()     { return "1.0.0"; }
            @Override public String description() {
                return "楼盘/客户/带看/认购/签约/回款全链路，适用于新房销售场景";
            }
            @Override public String vendor()      { return "MasterLife"; }
            @Override public String icon()        { return "BusinessOutline"; }
        };
    }

    @Override
    public List<MenuContribution> contributeMenus() {
        return List.of(
                // ---- 一级导航：房产管理 ----
                menu("/realty", "房产管理", "", "", "realty:view", "HomeOutline", 50),

                // ---- K2：楼盘与房源管理 ----
                menu("/realty/project", "楼盘管理", "/realty",
                        "realty/project/index", "realty:project:list", "BuildingOutline", 51),
                menu("/realty/house-type", "户型管理", "/realty",
                        "realty/houseType/index", "realty:houseType:list", "GridOutline", 52),
                menu("/realty/unit-status", "房源状态图", "/realty",
                        "realty/unitStatus/index", "realty:unitStatus:view", "MapOutline", 53),

                // ---- K3：带看 SOP ----
                menu("/realty/visit-sop", "带看 SOP", "/realty",
                        "realty/visit/sop", "realty:visit:list", "WalkOutline", 54),

                // ---- K2/K5：客户与成交 ----
                menu("/realty/customer", "房产客户", "/realty",
                        "realty/customer/index", "realty:customer:list", "PeopleOutline", 55),
                menu("/realty/deal", "认购签约", "/realty",
                        "realty/deal/index", "realty:deal:list", "DocumentTextOutline", 56),

                // ---- K5：联盟商与佣金 ----
                menu("/realty/alliance", "联盟商管理", "/realty",
                        "realty/alliance/index", "realty:alliance:list", "BusinessOutline", 57),
                menu("/realty/commission", "佣金结算", "/realty",
                        "realty/commission/index", "realty:commission:list", "CashOutline", 58),
                menu("/realty/payment", "回款管理", "/realty",
                        "realty/payment/index", "realty:payment:list", "CardOutline", 59)
        );
    }

    @Override
    public List<String> contributeDashboardCardCodes() {
        // 看板卡片由 K4 实现，此处仅声明 code 列表（启用插件时这些卡片对该租户可见）
        return List.of(
                "realty.unit-sales-rate",   // 房源去化率
                "realty.visit-funnel",      // 客户到访漏斗
                "realty.channel-roi"        // 渠道 ROI
        );
    }

    @Override
    public List<FieldTemplateContribution> contributeFieldTemplates() {
        return List.of(
                customerFieldTemplate()
        );
    }

    @Override
    public void onEnable(Long tenantId) {
        log.info("[RealtyPlugin] 房产插件在租户 {} 启用", tenantId);
    }

    @Override
    public void onDisable(Long tenantId) {
        log.info("[RealtyPlugin] 房产插件在租户 {} 禁用", tenantId);
    }

    // ---------------------------------------------------------------- 工具方法

    /**
     * 构建客户实体的房产扩展字段模板。
     */
    private FieldTemplateContribution customerFieldTemplate() {
        return new FieldTemplateContribution() {
            @Override public String entityType()    { return "customer"; }
            @Override public String templateName()  { return "房产客户扩展字段"; }
            @Override public List<FieldDef> fields() {
                return List.of(
                        field("intentArea",      "意向区域",   "text",   null),
                        field("budget",          "购房预算(万)", "number", null),
                        field("preferredFloor",  "意向楼层",   "text",   null),
                        field("houseUsage",      "购房用途",   "select",
                                "{\"options\":[\"自住\",\"投资\",\"商住两用\"]}"),
                        field("preferredRooms",  "意向户型",   "select",
                                "{\"options\":[\"一室\",\"两室\",\"三室\",\"四室及以上\"]}"),
                        field("loanType",        "付款方式",   "select",
                                "{\"options\":[\"全款\",\"按揭\",\"公积金贷款\",\"组合贷款\"]}"),
                        field("targetProject",   "意向楼盘",   "text",   null),
                        field("visitCount",      "带看次数",   "number", null),
                        field("subscribeDate",   "认购日期",   "date",   null)
                );
            }
        };
    }

    private static MenuContribution menu(String path, String name, String parent,
                                          String component, String permission,
                                          String icon, int sort) {
        return new MenuContribution() {
            @Override public String path()       { return path; }
            @Override public String name()       { return name; }
            @Override public String parent()     { return parent; }
            @Override public String component()  { return component; }
            @Override public String permission() { return permission; }
            @Override public String icon()       { return icon; }
            @Override public int sort()          { return sort; }
        };
    }

    private static FieldTemplateContribution.FieldDef field(String key, String label,
                                                              String fieldType, String config) {
        return new FieldTemplateContribution.FieldDef() {
            @Override public String key()       { return key; }
            @Override public String label()     { return label; }
            @Override public String fieldType() { return fieldType; }
            @Override public String config()    { return config; }
        };
    }
}
