package com.pengcheng.bi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.bi.dto.DimensionDef;
import com.pengcheng.bi.dto.MetricDef;
import com.pengcheng.bi.model.entity.BiViewModel;
import com.pengcheng.bi.model.mapper.BiViewModelMapper;
import com.pengcheng.bi.service.impl.BiViewModelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link BiViewModelServiceImpl} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class BiViewModelServiceImplTest {

    @Mock
    private BiViewModelMapper viewModelMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private BiViewModelServiceImpl service;

    private BiViewModel vm;

    @BeforeEach
    void setUp() {
        vm = new BiViewModel();
        vm.setId(1L);
        vm.setCode("customer_pool");
        vm.setName("客户池分析");
        vm.setBaseTable("customer");
        vm.setEnabled(1);
        vm.setDimensionsJson(
                "[{\"key\":\"source\",\"label\":\"来源\",\"column\":\"source\",\"type\":\"string\"},"
                + "{\"key\":\"city\",\"label\":\"城市\",\"column\":\"city\",\"type\":\"string\"}]");
        vm.setMetricsJson(
                "[{\"key\":\"count\",\"label\":\"客户数\",\"formula\":\"COUNT\",\"column\":\"id\"}]");
    }

    @Test
    @DisplayName("findByCode：存在时返回视图模型实体")
    void test_findByCode_found() {
        when(viewModelMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(vm);

        BiViewModel result = service.findByCode("customer_pool");

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("customer_pool");
    }

    @Test
    @DisplayName("findByCode：不存在时返回 null")
    void test_findByCode_notFound() {
        when(viewModelMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BiViewModel result = service.findByCode("not_exist");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("parseDimensions：正确解析 JSON 为 DimensionDef 列表")
    void test_parseDimensions_success() {
        List<DimensionDef> dims = service.parseDimensions(vm);

        assertThat(dims).hasSize(2);
        assertThat(dims.get(0).getKey()).isEqualTo("source");
        assertThat(dims.get(0).getType()).isEqualTo("string");
        assertThat(dims.get(1).getKey()).isEqualTo("city");
    }

    @Test
    @DisplayName("parseMetrics：正确解析 JSON 为 MetricDef 列表")
    void test_parseMetrics_success() {
        List<MetricDef> mets = service.parseMetrics(vm);

        assertThat(mets).hasSize(1);
        assertThat(mets.get(0).getKey()).isEqualTo("count");
        assertThat(mets.get(0).getFormula()).isEqualTo("COUNT");
    }

    @Test
    @DisplayName("parseDimensions：JSON 格式异常时抛 IllegalStateException")
    void test_parseDimensions_malformedJson_throwsIllegalState() {
        vm.setDimensionsJson("{invalid json}");

        assertThatThrownBy(() -> service.parseDimensions(vm))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("customer_pool");
    }
}
