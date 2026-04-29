package com.pengcheng.system.smarttable.relation;

/**
 * 关联字段配置 DTO
 *
 * 存储在 SmartTableField.options 中，key 对应：
 *   options.targetTableId  — 目标表格 ID
 *   options.displayField   — 展示字段 key（显示给用户）
 *   options.lookupField    — 匹配字段 key（在目标表中用于匹配）
 *
 * 示例（JSON options）：
 * {
 *   "targetTableId": 42,
 *   "displayField":  "name",
 *   "lookupField":   "id",
 *   "matchOn":       "customerId"   ← 当前行用于匹配的字段 key
 * }
 */
public class RelationConfig {

    /** 目标表格 ID */
    private Long targetTableId;

    /** 目标表中用于展示的字段 key */
    private String displayField;

    /**
     * 目标表中用于匹配的字段 key（通常是主键或唯一键）
     * 若为 null，则默认匹配 "id"
     */
    private String lookupField;

    /**
     * 当前行中用于提供匹配值的字段 key
     * 即：current_row[matchOn] == target_row[lookupField]
     */
    private String matchOn;

    // ========================= 构造 =========================

    public RelationConfig() {}

    public RelationConfig(Long targetTableId, String displayField, String lookupField, String matchOn) {
        this.targetTableId = targetTableId;
        this.displayField  = displayField;
        this.lookupField   = lookupField != null ? lookupField : "id";
        this.matchOn       = matchOn;
    }

    // ========================= Getters / Setters =========================

    public Long getTargetTableId() { return targetTableId; }
    public void setTargetTableId(Long targetTableId) { this.targetTableId = targetTableId; }

    public String getDisplayField() { return displayField; }
    public void setDisplayField(String displayField) { this.displayField = displayField; }

    public String getLookupField() { return lookupField != null ? lookupField : "id"; }
    public void setLookupField(String lookupField) { this.lookupField = lookupField; }

    public String getMatchOn() { return matchOn; }
    public void setMatchOn(String matchOn) { this.matchOn = matchOn; }

    @Override
    public String toString() {
        return "RelationConfig{targetTableId=" + targetTableId
                + ", displayField='" + displayField + '\''
                + ", lookupField='" + lookupField + '\''
                + ", matchOn='" + matchOn + "'}";
    }
}
