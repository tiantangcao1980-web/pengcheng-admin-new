package com.pengcheng.system.smarttable.relation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.smarttable.entity.SmartTableField;
import com.pengcheng.system.smarttable.entity.SmartTableRecord;
import com.pengcheng.system.smarttable.mapper.SmartTableFieldMapper;
import com.pengcheng.system.smarttable.mapper.SmartTableRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关联字段解析器
 *
 * 当字段类型 = "relation" 时，从目标表批量查询关联记录，
 * 并将 displayField 的值写入当前记录的 data Map。
 *
 * N+1 风险缓解策略（批量优先）：
 *   1. 聚合：扫描所有记录，收集需要匹配的 matchOn 值集合（去重）
 *   2. 一次 IN 查询：每个 relation 字段仅执行一次全量查询（而非每行一次）
 *   3. 内存 Map：查询结果按 lookupField 值建 Map，O(1) 写入
 *
 * 最坏情况：M 个 relation 字段 → M 次全量目标表查询。
 * 如目标表行数 > 1w，建议在调用方加 Redis 缓存层。
 *
 * 设计说明：fetchTargetRecords 设为 protected，允许测试子类直接覆盖，
 * 彻底规避 Mapper 接口在单元测试中的 Proxy 问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelationFieldResolver {

    private final SmartTableRecordMapper recordMapper;
    private final SmartTableFieldMapper  fieldMapper;

    // ========================= 批量解析入口 =========================

    /**
     * 批量富化关联字段（原地修改 record.data）
     *
     * @param records 当前表的记录列表
     * @param fields  当前表的字段定义
     */
    public void resolve(List<SmartTableRecord> records, List<SmartTableField> fields) {
        if (records == null || records.isEmpty() || fields == null) return;

        List<SmartTableField> relationFields = fields.stream()
                .filter(f -> "relation".equals(f.getFieldType()))
                .collect(Collectors.toList());

        if (relationFields.isEmpty()) return;

        for (SmartTableField rf : relationFields) {
            resolveField(records, rf);
        }
    }

    // ========================= 单字段批量解析 =========================

    private void resolveField(List<SmartTableRecord> records, SmartTableField field) {
        RelationConfig cfg = parseConfig(field);
        if (cfg == null || cfg.getTargetTableId() == null || cfg.getMatchOn() == null) {
            log.warn("关联字段 {} 配置不完整，跳过解析", field.getFieldKey());
            return;
        }

        // 收集匹配值
        Set<Object> matchValues = new HashSet<>();
        for (SmartTableRecord r : records) {
            if (r.getData() != null) {
                Object v = r.getData().get(cfg.getMatchOn());
                if (v != null) matchValues.add(v);
            }
        }
        if (matchValues.isEmpty()) return;

        // 一次查询目标表全量记录
        List<SmartTableRecord> targetRecords = fetchTargetRecords(cfg.getTargetTableId());

        // 建立 lookupField → displayField 的内存 Map
        Map<Object, Object> lookupMap = buildLookupMap(targetRecords, cfg);

        // 写入当前记录
        for (SmartTableRecord r : records) {
            if (r.getData() == null) continue;
            Object matchVal = r.getData().get(cfg.getMatchOn());
            if (matchVal == null) continue;
            Object displayVal = lookupMap.get(normalizeKey(matchVal));
            if (displayVal != null) {
                r.getData().put(field.getFieldKey(), displayVal);
            }
        }
    }

    // ========================= 目标表查询（可被测试子类覆盖） =========================

    /**
     * 查询目标表全量记录。
     * 设为 protected 便于测试覆盖，避免 Mapper 接口代理问题。
     */
    protected List<SmartTableRecord> fetchTargetRecords(Long targetTableId) {
        LambdaQueryWrapper<SmartTableRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmartTableRecord::getTableId, targetTableId);
        return recordMapper.selectList(wrapper);
    }

    // ========================= 内部工具方法 =========================

    /**
     * 将目标记录按 lookupField 建立索引 Map
     */
    private Map<Object, Object> buildLookupMap(List<SmartTableRecord> targets, RelationConfig cfg) {
        Map<Object, Object> map = new HashMap<>();
        for (SmartTableRecord t : targets) {
            if (t.getData() == null) continue;
            Object key  = t.getData().get(cfg.getLookupField());
            Object disp = t.getData().get(cfg.getDisplayField());
            // 若 lookupField = "id" 且 data 中无 id，退回用主键
            if (key == null && "id".equals(cfg.getLookupField())) {
                key = t.getId();
            }
            if (key != null && disp != null) {
                map.put(normalizeKey(key), disp);
            }
        }
        return map;
    }

    /**
     * 从字段 options 反序列化 RelationConfig
     */
    RelationConfig parseConfig(SmartTableField field) {
        if (field.getOptions() == null) return null;
        Map<String, Object> opts = field.getOptions();
        try {
            Object tid = opts.get("targetTableId");
            if (tid == null) return null;
            RelationConfig cfg = new RelationConfig();
            cfg.setTargetTableId(toLong(tid));
            cfg.setDisplayField(toStr(opts.get("displayField")));
            cfg.setLookupField(toStr(opts.getOrDefault("lookupField", "id")));
            cfg.setMatchOn(toStr(opts.get("matchOn")));
            return cfg;
        } catch (Exception e) {
            log.warn("解析关联字段配置失败 fieldKey={} err={}", field.getFieldKey(), e.getMessage());
            return null;
        }
    }

    private Object normalizeKey(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        return v;
    }

    private Long toLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }

    private String toStr(Object v) {
        return v == null ? null : v.toString();
    }
}
