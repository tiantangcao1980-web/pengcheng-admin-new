package com.pengcheng.ai.report.weekly;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.ai.provider.LlmProviderRouter;
import com.pengcheng.message.entity.SysNotice;
import com.pengcheng.message.service.SysNoticeService;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.entity.CustomerVisit;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.CustomerVisitMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AI 销售周报服务（位于 pengcheng-ai 模块以避免与 realty 循环依赖）
 *
 * 流程：
 *   1. 拉取上周关键经营数据（成交数 / 成交金额 / 新增客户 / 拜访数）
 *   2. 构造提示词调 LlmProviderRouter（V1.0 Sprint B 第一任务的 Provider 抽象，
 *      支持 dashscope / ollama 私有化切换）
 *   3. AI 输出周报文本，失败时降级为模板
 *   4. 落入 sys_notice 表（系统公告，所有人可见）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesWeeklyReportService {

    private final CustomerDealMapper dealMapper;
    private final RealtyCustomerMapper customerMapper;
    private final CustomerVisitMapper visitMapper;
    private final LlmProviderRouter llmRouter;
    private final SysNoticeService sysNoticeService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_DATE;

    private static final String SYSTEM_PROMPT = """
            你是一名资深房产销售运营顾问。基于给定的本周经营数据，
            请生成一份精炼周报（200-400 字），包含：
            1) 关键指标摘要；
            2) 同环比简评（若数据中有上周对比，可定性描述）；
            3) 本周亮点 1-2 条；
            4) 下周建议 2-3 条。
            语气专业、克制；不杜撰具体客户姓名。
            """;

    /** 生成"上周"周报并写入系统公告（每周一定时调用） */
    public String generateAndPublishLastWeekReport() {
        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.with(DayOfWeek.MONDAY).minusWeeks(1);
        LocalDate lastSunday = lastMonday.plusDays(6);
        return generateAndPublish(lastMonday, lastSunday);
    }

    /** 生成指定区间周报，落入 sys_notice */
    public String generateAndPublish(LocalDate weekStart, LocalDate weekEnd) {
        WeeklyMetrics metrics = collectMetrics(weekStart, weekEnd);
        String userPrompt = buildPrompt(weekStart, weekEnd, metrics);
        log.info("[销售周报] 区间 {}~{} 调 AI 生成", weekStart, weekEnd);

        String aiContent;
        try {
            aiContent = llmRouter.generate(SYSTEM_PROMPT, userPrompt);
        } catch (Exception e) {
            log.warn("[销售周报] AI 生成失败，使用降级模板: {}", e.getMessage());
            aiContent = fallbackTemplate(weekStart, weekEnd, metrics);
        }
        if (aiContent == null || aiContent.isBlank()) {
            aiContent = fallbackTemplate(weekStart, weekEnd, metrics);
        }

        publishNotice(weekStart, weekEnd, aiContent);
        return aiContent;
    }

    /** 收集经营指标 */
    WeeklyMetrics collectMetrics(LocalDate from, LocalDate to) {
        List<CustomerDeal> deals = dealMapper.selectList(new LambdaQueryWrapper<CustomerDeal>()
                .ge(CustomerDeal::getDealTime, from.atStartOfDay())
                .le(CustomerDeal::getDealTime, to.plusDays(1).atStartOfDay()));

        BigDecimal totalAmount = deals.stream()
                .map(d -> d.getDealAmount() == null ? BigDecimal.ZERO : d.getDealAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long newCustomers = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .ge(Customer::getCreateTime, from.atStartOfDay())
                .le(Customer::getCreateTime, to.plusDays(1).atStartOfDay()));

        Long visitCount = visitMapper.selectCount(new LambdaQueryWrapper<CustomerVisit>()
                .ge(CustomerVisit::getActualVisitTime, from.atStartOfDay())
                .le(CustomerVisit::getActualVisitTime, to.plusDays(1).atStartOfDay()));

        return new WeeklyMetrics(deals.size(), totalAmount,
                newCustomers == null ? 0 : newCustomers.intValue(),
                visitCount == null ? 0 : visitCount.intValue());
    }

    private String buildPrompt(LocalDate from, LocalDate to, WeeklyMetrics m) {
        return String.format("""
                本周经营数据（%s 至 %s）：
                - 成交单数：%d
                - 成交总额：%s 元
                - 新增客户：%d 人
                - 客户拜访：%d 次
                请按系统提示生成周报。
                """, from.format(DATE_FMT), to.format(DATE_FMT),
                m.dealCount(), m.totalAmount().toPlainString(),
                m.newCustomers(), m.visitCount());
    }

    private String fallbackTemplate(LocalDate from, LocalDate to, WeeklyMetrics m) {
        return String.format("""
                销售周报（%s ~ %s）

                关键指标：
                - 成交 %d 单，金额 %s 元
                - 新增客户 %d 人
                - 客户拜访 %d 次

                注：AI 服务暂不可用，本报告为系统降级版本。
                """, from.format(DATE_FMT), to.format(DATE_FMT),
                m.dealCount(), m.totalAmount().toPlainString(),
                m.newCustomers(), m.visitCount());
    }

    private void publishNotice(LocalDate weekStart, LocalDate weekEnd, String content) {
        SysNotice notice = new SysNotice();
        notice.setTitle(String.format("销售周报 %s ~ %s",
                weekStart.format(DATE_FMT), weekEnd.format(DATE_FMT)));
        notice.setContent(content);
        notice.setNoticeType(2);  // 2 = 周报类
        notice.setStatus(1);      // 1 = 已发布
        try {
            sysNoticeService.create(notice);
            log.info("[销售周报] 已写入系统公告 title={}", notice.getTitle());
        } catch (Exception e) {
            log.warn("[销售周报] 写入系统公告失败（不影响报告生成）: {}", e.getMessage());
        }
    }

    /** 周经营指标 */
    public record WeeklyMetrics(int dealCount, BigDecimal totalAmount,
                                int newCustomers, int visitCount) {}
}
