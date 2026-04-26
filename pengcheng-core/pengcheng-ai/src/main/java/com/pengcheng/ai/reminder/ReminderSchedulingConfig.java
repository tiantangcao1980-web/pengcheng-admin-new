package com.pengcheng.ai.reminder;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 仅在 ai 模块内启用 Spring 调度。
 * <p>
 * 红线：不动 pengcheng-job 的 Quartz 调度，不重复 @EnableScheduling 已有声明
 * （Spring 容许多次声明，最终只生效一次）。
 */
@Configuration
@EnableScheduling
public class ReminderSchedulingConfig {
}
