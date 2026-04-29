package com.pengcheng.system.smarttable.relation;

import com.pengcheng.system.smarttable.entity.SmartTableField;
import com.pengcheng.system.smarttable.entity.SmartTableRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * RelationFieldResolver 单元测试 — 4 用例
 *
 * 使用手写子类（TestableResolver）覆盖 fetchTargetRecords，
 * 完全规避 Mapper 接口代理问题（参考项目 AutomationEngineTest 风格）。
 *
 * 覆盖：正常关联解析、多条记录批量匹配、配置缺失跳过、parseConfig 正确反序列化
 */
@DisplayName("RelationFieldResolver — 关联字段解析器测试")
class RelationFieldResolverTest {

    // ========================= 可测试子类（覆盖 DB 访问） =========================

    /**
     * 覆盖 fetchTargetRecords，用内存 Map 替换实际 DB 查询。
     * 不依赖任何 Mapper，彻底纯内存测试。
     */
    static class TestableResolver extends RelationFieldResolver {

        /** targetTableId → 该表的伪记录列表 */
        final Map<Long, List<SmartTableRecord>> targetData = new HashMap<>();

        TestableResolver() {
            // RelationFieldResolver 构造器需要 mapper，传 null 即可（不会被调用）
            super(null, null);
        }

        @Override
        protected List<SmartTableRecord> fetchTargetRecords(Long targetTableId) {
            return targetData.getOrDefault(targetTableId, Collections.emptyList());
        }
    }

    private TestableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TestableResolver();
    }

    // ========================= TC-R01: 正常关联解析 =========================

    @Test
    @DisplayName("TC-R01: 单条记录关联字段正确解析 displayField 值")
    void tc01_normalResolution() {
        // 目标表（公司表）tableId = 100
        SmartTableRecord company = makeRecord(100L, Map.of("id", 1L, "name", "阿里巴巴"));
        resolver.targetData.put(100L, List.of(company));

        // 当前表记录（合同表）
        SmartTableRecord contract = makeRecord(1L, new HashMap<>(Map.of("companyId", 1L, "amount", 50000)));

        SmartTableField relationField = makeRelationField("companyName",
                Map.of("targetTableId", 100L, "displayField", "name",
                       "lookupField", "id", "matchOn", "companyId"));

        resolver.resolve(List.of(contract), List.of(relationField));

        assertThat(contract.getData().get("companyName")).isEqualTo("阿里巴巴");
    }

    // ========================= TC-R02: 多条记录批量匹配 =========================

    @Test
    @DisplayName("TC-R02: 多条记录批量关联，各自写入正确的 displayField 值")
    void tc02_batchResolution() {
        resolver.targetData.put(100L, List.of(
                makeRecord(100L, Map.of("id", 1L, "name", "腾讯")),
                makeRecord(100L, Map.of("id", 2L, "name", "字节"))
        ));

        SmartTableRecord r1 = makeRecord(1L, new HashMap<>(Map.of("companyId", 1L)));
        SmartTableRecord r2 = makeRecord(2L, new HashMap<>(Map.of("companyId", 2L)));
        SmartTableRecord r3 = makeRecord(3L, new HashMap<>(Map.of("companyId", 99L))); // 无匹配

        SmartTableField rf = makeRelationField("companyName",
                Map.of("targetTableId", 100L, "displayField", "name",
                       "lookupField", "id", "matchOn", "companyId"));

        resolver.resolve(List.of(r1, r2, r3), List.of(rf));

        assertThat(r1.getData().get("companyName")).isEqualTo("腾讯");
        assertThat(r2.getData().get("companyName")).isEqualTo("字节");
        assertThat(r3.getData().get("companyName")).isNull(); // 无匹配时不写入
    }

    // ========================= TC-R03: 配置缺失时跳过 =========================

    @Test
    @DisplayName("TC-R03: relation 字段缺少 targetTableId/matchOn 时跳过，不抛异常")
    void tc03_incompleteConfigSkipped() {
        SmartTableRecord r = makeRecord(1L, new HashMap<>(Map.of("x", 1)));

        // 缺少 targetTableId 和 matchOn
        SmartTableField rf = makeRelationField("linked",
                Map.of("displayField", "name"));

        assertThatCode(() -> resolver.resolve(List.of(r), List.of(rf))).doesNotThrowAnyException();
        assertThat(r.getData().get("linked")).isNull();
    }

    // ========================= TC-R04: parseConfig 正确反序列化 =========================

    @Test
    @DisplayName("TC-R04: parseConfig 正确解析 options 中的配置字段")
    void tc04_parseConfig() {
        SmartTableField field = makeRelationField("col",
                Map.of("targetTableId", 42L, "displayField", "title",
                       "lookupField", "uid", "matchOn", "userId"));

        RelationConfig cfg = resolver.parseConfig(field);

        assertThat(cfg).isNotNull();
        assertThat(cfg.getTargetTableId()).isEqualTo(42L);
        assertThat(cfg.getDisplayField()).isEqualTo("title");
        assertThat(cfg.getLookupField()).isEqualTo("uid");
        assertThat(cfg.getMatchOn()).isEqualTo("userId");
    }

    // ========================= 工厂方法 =========================

    private SmartTableRecord makeRecord(Long tableId, Map<String, Object> data) {
        SmartTableRecord r = new SmartTableRecord();
        r.setTableId(tableId);
        r.setData(new HashMap<>(data));
        return r;
    }

    private SmartTableField makeRelationField(String fieldKey, Map<String, Object> options) {
        SmartTableField f = new SmartTableField();
        f.setFieldKey(fieldKey);
        f.setFieldType("relation");
        f.setOptions(new HashMap<>(options));
        return f;
    }
}
