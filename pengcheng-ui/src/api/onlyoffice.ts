import { request } from '@/utils/request'

export interface OnlyOfficeConfigResult {
  config: Record<string, any>
  serverUrl: string
}

/**
 * 获取 OnlyOffice 编辑器配置（含 JWT token）。
 * @param docId  文档 ID
 * @param mode   'edit' | 'view'，默认 edit
 */
export function getConfig(docId: number, mode: 'edit' | 'view' = 'edit'): Promise<OnlyOfficeConfigResult> {
  return request({ url: '/admin/onlyoffice/config', method: 'get', params: { docId, mode } })
}
