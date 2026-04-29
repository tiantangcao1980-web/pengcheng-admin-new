package com.pengcheng.ai.report.weekly;

import com.pengcheng.ai.provider.LlmProviderRouter;
import com.pengcheng.message.entity.SysNotice;
import com.pengcheng.message.service.SysNoticeService;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.CustomerVisitMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 销售周报服务单测
 * 不连真实 DB / LLM，只验证：
 *   - 数据拼装正确性
 *   - AI 调用 + 公告写入
 *   - AI 失败时降级模板
 */
@DisplayName("SalesWeeklyReportService — 销售周报")
class SalesWeeklyReportServiceTest {

    private CustomerDealMapper dealMapper;
    private RealtyCustomerMapper customerMapper;
    private CustomerVisitMapper visitMapper;
    private LlmProviderRouter llmRouter;
    private SysNoticeService sysNoticeService;
    private SalesWeeklyReportService service;

    @BeforeEach
    void setUp() {
        dealMapper = mock(CustomerDealMapper.class);
        customerMapper = mock(RealtyCustomerMapper.class);
        visitMapper = mock(CustomerVisitMapper.class);
        llmRouter = mock(LlmProviderRouter.class);
        sysNoticeService = mock(SysNoticeService.class);
        service = new SalesWeeklyReportService(
                dealMapper, customerMapper, visitMapper, llmRouter, sysNoticeService);
    }

    private CustomerDeal deal(BigDecimal amt) {
        return CustomerDeal.builder().dealAmount(amt).build();
    }

    @Test
    @DisplayName("正常生成：拼数据 → 调 AI → 写公告")
    void generate_happyPath() {
        when(dealMapper.selectList(any())).thenReturn(List.of(
                deal(new BigDecimal("1000000")),
                deal(new BigDecimal("2500000"))));
        when(customerMapper.selectCount(any())).thenReturn(15L);
        when(visitMapper.selectCount(any())).thenReturn(40L);
        when(llmRouter.generate(anyString(), anyString())).thenReturn("AI 生成的周报正文");

        String result = service.generateAndPublish(
                LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 27));

        assertThat(result).isEqualTo("AI 生成的周报正文");

        ArgumentCaptor<String> promptCap = ArgumentCaptor.forClass(String.class);
        verify(llmRouter).generate(anyString(), promptCap.capture());
        assertThat(promptCap.getValue())
                .contains("成交单数：2")
                .contains("3500000")
                .contains("新增客户：15")
                .contains("客户拜访：40");

        ArgumentCaptor<SysNotice> noticeCap = ArgumentCaptor.forClass(SysNotice.class);
        verify(sysNoticeService).create(noticeCap.capture());
        assertThat(noticeCap.getValue().getTitle()).contains("销售周报");
        assertThat(noticeCap.getValue().getContent()).isEqualTo("AI 生成的周报正文");
        assertThat(noticeCap.getValue().getNoticeType()).isEqualTo(2);
        assertThat(noticeCap.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("AI 抛异常：降级模板，不阻塞，公告仍写入")
    void generate_aiThrows_fallsBack() {
        when(dealMapper.selectList(any())).thenReturn(List.of());
        when(customerMapper.selectCount(any())).thenReturn(0L);
        when(visitMapper.selectCount(any())).thenReturn(0L);
        when(llmRouter.generate(anyString(), anyString()))
                .thenThrow(new RuntimeException("AI 不可用"));

        String result = service.generateAndPublish(
                LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 27));

        assertThat(result).contains("AI 服务暂不可用").contains("成交 0 单");
        verify(sysNoticeService).create(any(SysNotice.class));
    }

    @Test
    @DisplayName("AI 返回空：降级模板")
    void generate_aiReturnsBlank_fallsBack() {
        when(dealMapper.selectList(any())).thenReturn(List.of());
        when(customerMapper.selectCount(any())).thenReturn(0L);
        when(visitMapper.selectCount(any())).thenReturn(0L);
        when(llmRouter.generate(anyString(), anyString())).thenReturn("   ");

        String result = service.generateAndPublish(
                LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 27));

        assertThat(result).contains("AI 服务暂不可用");
    }

    @Test
    @DisplayName("写公告失败不阻塞，AI 内容仍返回")
    void publishNotice_failureGraceful() {
        when(dealMapper.selectList(any())).thenReturn(List.of());
        when(customerMapper.selectCount(any())).thenReturn(0L);
        when(visitMapper.selectCount(any())).thenReturn(0L);
        when(llmRouter.generate(anyString(), anyString())).thenReturn("正常 AI 输出");
        org.mockito.Mockito.doThrow(new RuntimeException("公告系统挂了"))
                .when(sysNoticeService).create(any());

        String result = service.generateAndPublish(
                LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 27));

        assertThat(result).isEqualTo("正常 AI 输出");
    }

    @Test
    @DisplayName("collectMetrics 拼装：成交金额累加正确")
    void collectMetrics_sumsCorrectly() {
        when(dealMapper.selectList(any())).thenReturn(List.of(
                deal(new BigDecimal("100")),
                deal(new BigDecimal("200")),
                deal(null),  // null amount safely skipped as zero
                deal(new BigDecimal("300"))));
        when(customerMapper.selectCount(any())).thenReturn(5L);
        when(visitMapper.selectCount(any())).thenReturn(10L);

        SalesWeeklyReportService.WeeklyMetrics m = service.collectMetrics(
                LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 27));

        assertThat(m.dealCount()).isEqualTo(4);
        assertThat(m.totalAmount()).isEqualByComparingTo(new BigDecimal("600"));
        assertThat(m.newCustomers()).isEqualTo(5);
        assertThat(m.visitCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("collectMetrics: count 为 null 时安全降级为 0")
    void collectMetrics_nullCount() {
        when(dealMapper.selectList(any())).thenReturn(List.of());
        when(customerMapper.selectCount(any())).thenReturn(null);
        when(visitMapper.selectCount(any())).thenReturn(null);

        SalesWeeklyReportService.WeeklyMetrics m = service.collectMetrics(
                LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 27));

        assertThat(m.newCustomers()).isEqualTo(0);
        assertThat(m.visitCount()).isEqualTo(0);
    }
}
