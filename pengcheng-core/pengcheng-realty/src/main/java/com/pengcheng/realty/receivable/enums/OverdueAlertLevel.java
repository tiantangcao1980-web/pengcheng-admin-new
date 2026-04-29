package com.pengcheng.realty.receivable.enums;

import lombok.Getter;

/**
 * 回款逾期告警档位
 *
 * 触发规则（daysOverdue = today - dueDate）：
 *   FIRST: daysOverdue >= 0  且 < 3  → 首次告警
 *   T3   : daysOverdue >= 3  且 < 7  → T+3 升级
 *   T7   : daysOverdue >= 7  且 < 15 → T+7 升级
 *   T15  : daysOverdue >= 15         → T+15 严重
 *
 * 同档位仅触发一次，跨档位升级才再次触发通知。
 * ReceivableAlert.notifyCount 语义：已发档位数（FIRST 已发=1，T3 已发=2 …）
 */
@Getter
public enum OverdueAlertLevel {

    FIRST(0, 0, "首次逾期"),
    T3(1, 3, "T+3 升级"),
    T7(2, 7, "T+7 升级"),
    T15(3, 15, "T+15 严重");

    /** 档位序号 0-3 */
    private final int order;

    /** 触发该档所需的最小逾期天数 */
    private final int minDays;

    /** 中文标签 */
    private final String label;

    OverdueAlertLevel(int order, int minDays, String label) {
        this.order = order;
        this.minDays = minDays;
        this.label = label;
    }

    /**
     * 根据逾期天数计算应当处于的档位（取最高满足档）。
     * @return 对应档位；若 daysOverdue < 0 返回 null（未到任何档位）
     */
    public static OverdueAlertLevel of(int daysOverdue) {
        if (daysOverdue >= T15.minDays) return T15;
        if (daysOverdue >= T7.minDays) return T7;
        if (daysOverdue >= T3.minDays) return T3;
        if (daysOverdue >= FIRST.minDays) return FIRST;
        return null;
    }
}
