import { request } from '@/utils/request'

/** 补卡申请单 */
export interface CorrectionItem {
  id?: number
  userId?: number
  correctionDate: string
  /** 1=上班 2=下班 */
  correctionType: number
  expectedTime: string
  reason?: string
  approvalInstanceId?: number
  /** 1=待审批 2=已通过 3=已驳回 */
  status?: number
  createTime?: string
}

export const oaCorrectionApi = {
  list(params?: { userId?: number; status?: number }) {
    return request<CorrectionItem[]>({ url: '/admin/oa/corrections', method: 'get', params })
  },
  detail(id: number) {
    return request<CorrectionItem>({ url: `/admin/oa/corrections/${id}`, method: 'get' })
  },
  submit(data: CorrectionItem & { flowDefId?: number }) {
    return request<number>({ url: '/admin/oa/corrections', method: 'post', data })
  },
}
