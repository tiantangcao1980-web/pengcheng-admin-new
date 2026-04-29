package com.pengcheng.bi.engine;

import com.pengcheng.bi.dto.DimensionDef;
import com.pengcheng.bi.dto.Filter;
import com.pengcheng.bi.dto.FilterOp;
import com.pengcheng.bi.dto.MetricDef;
import com.pengcheng.bi.dto.Sort;
import com.pengcheng.bi.engine.impl.JdbcBiQueryEngine;
import com.pengcheng.bi.model.entity.BiViewModel;
import com.pengcheng.bi.service.BiViewModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JdbcBiQueryEngine} 单元测试（全量 Mock，无 Spring 上下文）。
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>白名单通过：维度 + 指标正常查询，拼出的 SQL 含预期列表达式。</li>
 *   <li>非白名单 dimension key 拒绝。</li>
 *   <li>非白名单 metric key 拒绝。</li>
 *   <li>非白名单 filter.column 拒绝。</li>
 *   <li>SQL 注入尝试（filter value 含 {@code ';--}）通过参数化传递，SQL 中不含注入字符。</li>
 *   <li>limit 超 10000 被截断为 10000。</li>
 *   <li>多维度 GROUP BY 拼接顺序与请求维度顺序一致。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class JdbcBiQueryEngineTest {

    @Mock
    private BiViewModelService viewModelService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private JdbcBiQueryEngine engine;

    private BiViewModel sampleVm;
    private List<DimensionDef> sampleDims;
    private List<MetricDef> sampleMets;

    @BeforeEach
    void setUp() {
        sampleVm = new BiViewModel();
        sampleVm.setCode("customer_pool");
        sampleVm.setBaseTable("customer");
        sampleVm.setEnabled(1);
        sampleVm.setJoinClause(null);

        DimensionDef sourceD = new DimensionDef();
        sourceD.setKey("source");
        sourceD.setLabel("来源");
        sourceD.setColumn("source");
        sourceD.setType("string");

        DimensionDef monthD = new DimensionDef();
        monthD.setKey("createMonth");
        monthD.setLabel("创建月");
        monthD.setColumn("create_time");
        monthD.setType("date_month");

        DimensionDef cityD = new DimensionDef();
        cityD.setKey("city");
        cityD.setLabel("城市");
        cityD.setColumn("city");
        cityD.setType("string");

        sampleDims = List.of(sourceD, monthD, cityD);

        MetricDef countM = new MetricDef();
        countM.setKey("count");
        countM.setLabel("客户数");
        countM.setFormula("COUNT");
        countM.setColumn("id");

        MetricDef avgM = new MetricDef();
        avgM.setKey("avgVisits");
        avgM.setLabel("平均拜访");
        avgM.setFormula("AVG");
        avgM.setColumn("visit_count");

        sampleMets = List.of(countM, avgM);

        when(viewModelService.findByCode("customer_pool")).thenReturn(sampleVm);
        when(viewModelService.parseDimensions(sampleVm)).thenReturn(sampleDims);
        when(viewModelService.parseMetrics(sampleVm)).thenReturn(sampleMets);
    }

    // ====================================================================
    // 用例 1：白名单通过，拼出预期 SQL（含正确列表达式）
    // ====================================================================
    @Test
    @DisplayName("白名单校验通过：SELECT 含预期列表达式，GROUP BY 含维度列")
    void test_validDimAndMetric_generatesCorrectSql() {
        BiQueryRequest req = new BiQueryRequest();
        req.setViewCode("customer_pool");
        req.setDimensions(List.of("source"));
        req.setMetrics(List.of("count"));

        // Mock JdbcTemplate 返回空结果
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class)))
                .thenReturn(0L);

        engine.execute(req);

        // 捕获 SQL
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(Object[].class));
        String sql = sqlCaptor.getValue();

        // 维度表达式：`source` AS `source`
        assertThat(sql).contains("`source` AS `source`");
        // 指标聚合：COUNT(`id`)
        assertThat(sql).contains("COUNT(`id`) AS `count`");
        // GROUP BY 含维度列
        assertThat(sql).containsIgnoringCase("GROUP BY");
        assertThat(sql).contains("`source`");
        // FROM 正确表
        assertThat(sql).containsIgnoringCase("FROM customer");
    }

    // ====================================================================
    // 用例 2：非白名单 dimension key 拒绝
    // ====================================================================
    @Test
    @DisplayName("非白名单 dimension key 应抛 IllegalArgumentException")
    void test_invalidDimensionKey_throwsIllegalArgument() {
        BiQueryRequest req = new BiQueryRequest();
        req.setViewCode("customer_pool");
        req.setDimensions(List.of("hack_column"));
        req.setMetrics(List.of("count"));

        assertThatThrownBy(() -> engine.execute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hack_column");
    }

    // ====================================================================
    // 用例 3：非白名单 metric key 拒绝
    // ====================================================================
    @Test
    @DisplayName("非白名单 metric key 应抛 IllegalArgumentException")
    void test_invalidMetricKey_throwsIllegalArgument() {
        BiQueryRequest req = new BiQueryRequest();
        req.setViewCode("customer_pool");
        req.setDimensions(List.of("source"));
        req.setMetrics(List.of("evilMetric"));

        assertThatThrownBy(() -> engine.execute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evilMetric");
    }

    // ====================================================================
    // 用例 4：非白名单 filter.column 拒绝
    // ====================================================================
    @Test
    @DisplayName("非白名单 filter.column 应抛 IllegalArgumentException")
    void test_invalidFilterColumn_throwsIllegalArgument() {
        BiQueryRequest req = new BiQueryRequest();
        req.setViewCode("customer_pool");
        req.setDimensions(List.of("source"));
        req.setMetrics(List.of("count"));

        Filter f = new Filter();
        f.setColumn("sys_user");  // 非白名单
        f.setOp(FilterOp.EQ);
        f.setValues(List.of("1"));
        req.setFilters(List.of(f));

        assertThatThrownBy(() -> engine.execute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sys_user");
    }

    // ====================================================================
    // 用例 5：SQL 注入尝试 — filter value 含 ';-- 通过参数化传递，SQL 不含注入字符
    // ====================================================================
    @Test
    @DisplayName("SQL 注入 filter value 通过参数化传递，SQL 语句本身不含注入字符串")
    void test_sqlInjectionInFilterValue_parameterized() {
        BiQueryRequest req = new BiQueryRequest();
        req.setViewCode("customer_pool");
        req.setDimensions(List.of("source"));
        req.setMetrics(List.of("count"));

        String maliciousValue = "'; DROP TABLE customer; --";
        Filter f = new Filter();
        f.setColumn("source");      // 在白名单中
        f.setOp(FilterOp.EQ);
        f.setValues(List.of(maliciousValue));
        req.setFilters(List.of(f));

        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class)))
                .thenReturn(0L);

        engine.execute(req);

        // SQL 语句只含 ?，不含恶意字符串
        ArgumentCaptor<String> sqlCaptor   = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), paramCaptor.capture());

        String sql = sqlCaptor.getValue();
        assertThat(sql).doesNotContain("DROP TABLE");
        assertThat(sql).doesNotContain("--");
        assertThat(sql).contains("?");

        // 恶意字符串只存在于参数数组
        boolean foundInParams = false;
        for (Object param : paramCaptor.getValue()) {
            if (maliciousValue.equals(param)) {
                foundInParams = true;
                break;
            }
        }
        assertThat(foundInParams).as("恶意值应在参数数组中，而非 SQL 字符串").isTrue();
    }

    // ====================================================================
    // 用例 6：limit 超 10000 截断为 10000
    // ====================================================================
    @Test
    @DisplayName("limit 超过 10000 时应被强制截断为 10000")
    void test_limitExceedMax_clampedTo10000() {
        BiQueryRequest req = new BiQueryRequest();
        req.setViewCode("customer_pool");
        req.setDimensions(List.of("source"));
        req.setMetrics(List.of("count"));
        req.setLimit(99999);

        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class)))
                .thenReturn(0L);

        engine.execute(req);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(Object[].class));

        String sql = sqlCaptor.getValue();
        assertThat(sql).containsIgnoringCase("LIMIT " + JdbcBiQueryEngine.MAX_LIMIT);
        assertThat(sql).doesNotContain("LIMIT 99999");
    }

    // ====================================================================
    // 用例 7：多维度 GROUP BY 顺序与请求维度顺序一致
    // ====================================================================
    @Test
    @DisplayName("多维度 GROUP BY 拼接顺序应与请求 dimensions 列表顺序一致")
    void test_multiDimension_groupByOrder() {
        BiQueryRequest req = new BiQueryRequest();
        req.setViewCode("customer_pool");
        req.setDimensions(List.of("source", "city", "createMonth"));  // 指定顺序
        req.setMetrics(List.of("count"));

        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class)))
                .thenReturn(0L);

        engine.execute(req);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(Object[].class));
        String sql = sqlCaptor.getValue();

        // GROUP BY 中：source 在 city 之前，city 在 DATE_FORMAT(create_time...) 之前
        int idxSource    = sql.indexOf("`source`");
        int idxCity      = sql.lastIndexOf("`city`");
        int idxDateFmt   = sql.indexOf("DATE_FORMAT");

        // 至少有两个维度出现了
        assertThat(idxSource).isGreaterThan(-1);
        assertThat(idxCity).isGreaterThan(-1);
        assertThat(idxDateFmt).isGreaterThan(-1);

        // 在 GROUP BY 之后的顺序：source < city < date_format
        int groupByIdx = sql.toUpperCase().indexOf("GROUP BY");
        String afterGroupBy = sql.substring(groupByIdx);
        int relSource  = afterGroupBy.indexOf("`source`");
        int relCity    = afterGroupBy.lastIndexOf("`city`");
        int relDateFmt = afterGroupBy.indexOf("DATE_FORMAT");
        assertThat(relSource).isLessThan(relCity);
        assertThat(relCity).isLessThan(relDateFmt);
    }
}
