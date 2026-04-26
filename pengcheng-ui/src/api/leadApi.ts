import { request } from '@/utils/request'

export interface CrmLead {
  id?: number
  leadNo?: string
  name: string
  phone?: string
  phoneMasked?: string
  email?: string
  wechat?: string
  company?: string
  source?: string
  sourceDetail?: string
  intentionLevel?: number
  status?: number
  ownerId?: number
  deptId?: number
  customerId?: number
  remark?: string
  createTime?: string
}

export interface LeadAssignDTO {
  leadIds: number[]
  ruleType?: 'manual' | 'round_robin' | 'load_balance' | 'rule'
  targetUserId?: number
  candidateUserIds?: number[]
  note?: string
}

export interface LeadConvertDTO {
  leadId: number
  customerId?: number
  customerName?: string
  remark?: string
}

/** V4.0 闭环③ - 线索 API */
export const leadApi = {
  page(params: { page?: number; size?: number; ownerId?: number; status?: number; keyword?: string }) {
    return request({ url: '/crm/leads', method: 'get', params })
  },
  get(id: number) {
    return request({ url: `/crm/leads/${id}`, method: 'get' })
  },
  create(data: Partial<CrmLead>) {
    return request({ url: '/crm/leads', method: 'post', data })
  },
  assign(data: LeadAssignDTO) {
    return request({ url: '/crm/leads/assign', method: 'post', data })
  },
  convert(data: LeadConvertDTO) {
    return request({ url: '/crm/leads/convert', method: 'post', data })
  },
  assignments(id: number) {
    return request({ url: `/crm/leads/${id}/assignments`, method: 'get' })
  },
  // 公开采集表单 ----
  createForm(form: { formCode: string; title: string; schemaJson: string }) {
    return request({ url: '/crm/lead-forms', method: 'post', data: form })
  },
  publicGetForm(code: string) {
    return request({ url: `/crm/lead-forms/public/${code}`, method: 'get' })
  },
  publicSubmit(formCode: string, fields: Record<string, any>) {
    return request({ url: '/crm/lead-forms/public/submit', method: 'post', data: { formCode, fields } })
  }
}
