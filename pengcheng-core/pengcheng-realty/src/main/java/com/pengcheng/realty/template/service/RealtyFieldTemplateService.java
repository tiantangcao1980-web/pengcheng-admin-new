package com.pengcheng.realty.template.service;

import com.pengcheng.realty.template.entity.CustomFieldTemplateGroup;

import java.util.List;

/**
 * 行业字段模板服务。
 * <p>
 * 通过 pluginCode + entityType 定位模板分组，批量修改对应 custom_field_def
 * 的 enabled 状态，实现插件字段的一键启用/禁用。
 * </p>
 * <p>
 * <b>K1 调用约定</b>：RealtyIndustryPlugin.onEnable(tenantId) 依次调用：
 * <pre>
 *   applyTemplate("realty", "lead",        tenantId);
 *   applyTemplate("realty", "customer",    tenantId);
 *   applyTemplate("realty", "opportunity", tenantId);
 * </pre>
 * onDisable(tenantId) 调用对应的 revokeTemplate(...)。
 * </p>
 */
public interface RealtyFieldTemplateService {

    /**
     * 启用模板：将模板分组内的所有 field_key 对应的 custom_field_def 设为 enabled=1。
     *
     * @param pluginCode 插件编码，如 {@code realty}
     * @param entityType 实体类型，如 {@code lead}
     * @param tenantId   租户 ID
     *                   <p>
     *                   TODO(Phase-6): 当前实现仅维护全局字段定义（custom_field_def 无 tenant 分区列），
     *                   多租户字段隔离待 Phase 6 在 custom_field_def 加 tenant_id 列后补齐。
     *                   </p>
     * @throws IllegalArgumentException 若 pluginCode + entityType 无对应模板分组
     */
    void applyTemplate(String pluginCode, String entityType, Long tenantId);

    /**
     * 禁用模板：将模板分组内的所有 field_key 对应的 custom_field_def 设为 enabled=0。
     *
     * @param pluginCode 插件编码
     * @param entityType 实体类型
     * @param tenantId   租户 ID（见 applyTemplate 的 TODO 说明）
     * @throws IllegalArgumentException 若 pluginCode + entityType 无对应模板分组
     */
    void revokeTemplate(String pluginCode, String entityType, Long tenantId);

    /**
     * 列出指定插件的所有模板分组。
     *
     * @param pluginCode 插件编码
     * @return 模板分组列表，按 entity_type 升序
     */
    List<CustomFieldTemplateGroup> listGroups(String pluginCode);
}
