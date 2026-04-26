import { request } from '@/utils/request'

export interface CustomerTag {
  id?: number
  tagName: string
  color?: string
  category?: string
  description?: string
  sortOrder?: number
  enabled?: number
}

/** V4.0 闭环③ - 客户标签 API */
export const crmTagApi = {
  list() {
    return request({ url: '/crm/customer-tags', method: 'get' })
  },
  create(tag: CustomerTag) {
    return request({ url: '/crm/customer-tags', method: 'post', data: tag })
  },
  bind(customerId: number, tagIds: number[]) {
    return request({ url: `/crm/customer-tags/customer/${customerId}`, method: 'put', data: tagIds })
  },
  ofCustomer(customerId: number) {
    return request({ url: `/crm/customer-tags/customer/${customerId}`, method: 'get' })
  }
}
