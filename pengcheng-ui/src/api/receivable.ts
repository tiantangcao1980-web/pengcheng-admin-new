import { request } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface ReceivablePlanRecord {
  id: number
  dealId: number
  periodNo: number
  periodName?: string
  dueDate: string
  dueAmount: number
  paidAmount: number
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ReceivableRecordRecord {
  id: number
  planId: number
  amount: number
  paidDate: string
  payWay: number
  payer?: string
  voucherNo?: string
  attachmentUrl?: string
  remark?: string
  createTime?: string
}

export interface ReceivableAlertRecord {
  id: number
  planId: number
  alertType: number
  alertTime?: string
  lastNotified?: string
  notifyCount?: number
  handled: number
  handledBy?: number
  handledAt?: string
  handledRemark?: string
  createTime?: string
}

/** 逾期告警档位（与后端 OverdueAlertLevel 对应）*/
export type OverdueAlertLevel = 'FIRST' | 'T3' | 'T7' | 'T15'

export const OVERDUE_LEVEL_LABEL: Record<OverdueAlertLevel, string> = {
  FIRST: '首次逾期',
  T3: 'T+3 升级',
  T7: 'T+7 升级',
  T15: 'T+15 严重'
}

/**
 * 由 notifyCount 推导当前档位：
 *   notifyCount=1 → FIRST  (level 0)
 *   notifyCount=2 → T3     (level 1)
 *   notifyCount=3 → T7     (level 2)
 *   notifyCount>=4 → T15   (level 3)
 */
export function levelFromNotifyCount(notifyCount?: number): OverdueAlertLevel | null {
  if (!notifyCount || notifyCount < 1) return null
  if (notifyCount >= 4) return 'T15'
  if (notifyCount === 3) return 'T7'
  if (notifyCount === 2) return 'T3'
  return 'FIRST'
}

export interface ReceivableStatsRecord {
  totalDue: number
  totalPaid: number
  totalUnpaid: number
  totalOverdue: number
  overdueCount: number
  totalCount: number
}

export interface ReceivablePlanCreateItem {
  periodNo: number | null
  periodName: string
  dueDate: string | null
  dueAmount: number | null
  remark?: string
}

export interface ReceivablePlanCreateParams {
  dealId: number | null
  items: ReceivablePlanCreateItem[]
}

export interface ReceivablePlanQueryParams {
  dealId?: number
  status?: number
  page?: number
  pageSize?: number
}

export interface ReceivableRecordCreateParams {
  planId: number
  amount: number
  paidDate: string | null
  payWay?: number
  payer?: string
  voucherNo?: string
  attachmentUrl?: string
  remark?: string
}

export const receivableApi = {
  createPlan(data: ReceivablePlanCreateParams) {
    return request<number[]>({
      url: '/admin/receivable/plan',
      method: 'post',
      data
    })
  },

  pagePlans(data: ReceivablePlanQueryParams) {
    return request<PageResult<ReceivablePlanRecord>>({
      url: '/admin/receivable/plan/page',
      method: 'post',
      data
    })
  },

  createRecord(data: ReceivableRecordCreateParams) {
    return request<number>({
      url: '/admin/receivable/record',
      method: 'post',
      data
    })
  },

  listRecords(planId: number) {
    return request<ReceivableRecordRecord[]>({
      url: `/admin/receivable/record/list/${planId}`,
      method: 'get'
    })
  },

  openAlerts() {
    return request<ReceivableAlertRecord[]>({
      url: '/admin/receivable/alert/open',
      method: 'get'
    })
  },

  stats() {
    return request<ReceivableStatsRecord>({
      url: '/admin/receivable/stats',
      method: 'get'
    })
  },

  runOverdueCheck() {
    return request<[number, number]>({
      url: '/admin/receivable/check/overdue',
      method: 'post'
    })
  }
}
