import { request } from '@/utils/request'

export interface ImportFailedRow {
  rowNum: number
  message: string
  row: Record<string, any>
}

export interface ImportResult {
  total: number
  success: number
  failed: number
  failedRows: ImportFailedRow[]
}

/** V4.0 闭环③ - Excel 导入导出 API */
export const crmImportApi = {
  /** 上传 Excel 导入线索；后端返回 ImportResult */
  importLeads(file: File) {
    const fd = new FormData()
    fd.append('file', file)
    return request({
      url: '/crm/import-export/leads/import',
      method: 'post',
      data: fd,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  /** 下载导入模板 */
  templateUrl(): string {
    return '/crm/import-export/leads/template'
  }
}
