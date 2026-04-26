import { request } from '@/utils/request'

/** 审批模板 */
export interface ApprovalTemplateItem {
  id?: number
  code: string
  name: string
  formSchema?: string
  defaultFlowDefId?: number
  /** 1=假勤 2=出差 3=费用 4=通用 */
  category: number
  enabled?: number
  remark?: string
  createTime?: string
}

export const oaApprovalTemplateApi = {
  list(params?: { enabled?: number }) {
    return request<ApprovalTemplateItem[]>({ url: '/admin/oa/approval-templates', method: 'get', params })
  },
  detail(id: number) {
    return request<ApprovalTemplateItem>({ url: `/admin/oa/approval-templates/${id}`, method: 'get' })
  },
  detailByCode(code: string) {
    return request<ApprovalTemplateItem>({ url: `/admin/oa/approval-templates/by-code/${code}`, method: 'get' })
  },
  create(data: ApprovalTemplateItem) {
    return request<number>({ url: '/admin/oa/approval-templates', method: 'post', data })
  },
  update(data: ApprovalTemplateItem) {
    return request<void>({ url: `/admin/oa/approval-templates/${data.id}`, method: 'put', data })
  },
  remove(id: number) {
    return request<void>({ url: `/admin/oa/approval-templates/${id}`, method: 'delete' })
  },
}
