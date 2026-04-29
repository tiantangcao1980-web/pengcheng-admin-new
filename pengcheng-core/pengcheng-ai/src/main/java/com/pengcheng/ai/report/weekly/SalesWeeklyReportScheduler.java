package com.pengcheng.ai.report.weekly;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 销售周报定时调度
 *
 * 默认每周一 09:00 执行（cron: 0 0 9 ? * MON）
 * 可通过 application.yml 覆盖：pengcheng.realty.weekly-report.cron
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesWeeklyReportScheduler {

    private final SalesWeeklyReportService weeklyReportService;

    @Scheduled(cron = "${pengcheng.realty.weekly-report.cron:0 0 9 ? * MON}")
    public void run() {
        try {
            String content = weeklyReportService.generateAndPublishLastWeekReport();
            log.info("[销售周报] 定时任务完成，长度={}", content == null ? 0 : content.length());
        } catch (Exception e) {
            log.error("[销售周报] 定时任务执行失败", e);
        }
    }
}
