import { request } from '@/utils/request'

export type CustomFieldType = 'text' | 'number' | 'date' | 'select' | 'multi_select' | 'file'

export interface CustomFieldDef {
  id?: number
  entityType: string
  fieldKey: string
  label: string
  fieldType: CustomFieldType
  required?: number
  defaultValue?: string
  optionsJson?: string
  validationJson?: string
  sortOrder?: number
  enabled?: number
}

/** V4.0 闭环③ - 自定义字段 API */
export const customFieldApi = {
  listDefs(entityType: string) {
    return request({ url: '/crm/custom-fields/defs', method: 'get', params: { entityType } })
  },
  createDef(def: CustomFieldDef) {
    return request({ url: '/crm/custom-fields/defs', method: 'post', data: def })
  },
  saveValues(entityType: string, entityId: number, values: Record<string, any>) {
    return request({
      url: '/crm/custom-fields/values',
      method: 'put',
      params: { entityType, entityId },
      data: values
    })
  },
  loadValues(entityType: string, entityId: number) {
    return request({
      url: '/crm/custom-fields/values',
      method: 'get',
      params: { entityType, entityId }
    })
  }
}
