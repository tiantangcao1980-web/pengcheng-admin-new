/**
 * V4.0 MVP 闭环④ — 智能提醒规则配置 API。
 * 后端契约：
 *   GET    /admin/ai/reminder/rules
 *   PUT    /admin/ai/reminder/rules/:id
 *   POST   /admin/ai/reminder/rules/:id/fire   立即触发（用于联调）
 */
import { request } from '@/utils/request'

export interface AiReminderRule {
  id: number
  ruleCode: string
  ruleName: string
  ruleType: 'DAILY' | 'THRESHOLD' | 'PRE_EXPIRE'
  cronExpr?: string | null
  thresholdMin?: number | null
  preDays?: number | null
  targetScope: string
  channel: string
  template: string
  enabled: 0 | 1
  lastFiredAt?: string | null
}

export const aiReminderApi = {
  list(): Promise<AiReminderRule[]> {
    return request<AiReminderRule[]>({
      url: '/admin/ai/reminder/rules',
      method: 'get'
    })
  },

  update(rule: AiReminderRule): Promise<void> {
    return request<void>({
      url: `/admin/ai/reminder/rules/${rule.id}`,
      method: 'put',
      data: rule
    })
  },

  fire(id: number): Promise<{ sent: number }> {
    return request<{ sent: number }>({
      url: `/admin/ai/reminder/rules/${id}/fire`,
      method: 'post'
    })
  }
}
