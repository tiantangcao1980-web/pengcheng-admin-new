import { request } from '@/utils/request'

export type InviteChannel = 'SMS' | 'LINK' | 'QRCODE' | 'EXCEL'

export interface TenantMemberInvite {
  id: number
  tenantId: number
  inviteCode: string
  channel: InviteChannel
  phone?: string
  email?: string
  deptId?: number
  roleIds?: string
  inviterId: number
  expiresAt: string
  status: number
  acceptedUserId?: number
  acceptedAt?: string
  failReason?: string
  createTime?: string
}

export interface InviteCreateParams {
  tenantId: number
  channel: InviteChannel
  phone?: string
  email?: string
  deptId?: number
  roleIds?: number[]
  expireHours?: number
}

export interface InviteImportRow {
  lineNo: number
  phone?: string
  success: boolean
  failReason?: string
  inviteId?: number
}

export interface InviteImportResult {
  totalCount: number
  successCount: number
  failCount: number
  rows: InviteImportRow[]
}

export const inviteApi = {
  /** 单条创建（SMS/LINK/QRCODE） */
  create(data: InviteCreateParams): Promise<TenantMemberInvite> {
    return request({
      url: '/auth/tenant/invite',
      method: 'post',
      data
    })
  },

  /** Excel/CSV 批量导入 */
  importInvites(tenantId: number, file: File): Promise<InviteImportResult> {
    const form = new FormData()
    form.append('tenantId', String(tenantId))
    form.append('file', file)
    return request({
      url: '/auth/tenant/invite/import',
      method: 'post',
      data: form,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  /** 列表 */
  list(tenantId: number, status?: number): Promise<TenantMemberInvite[]> {
    return request({
      url: '/auth/tenant/invite',
      method: 'get',
      params: { tenantId, status }
    })
  },

  /** 撤销 */
  revoke(id: number): Promise<void> {
    return request({
      url: `/auth/tenant/invite/${id}/revoke`,
      method: 'post'
    })
  },

  /** 通过 code 查（无需登录态） */
  getByCode(code: string): Promise<TenantMemberInvite> {
    return request({
      url: `/auth/tenant/invite/by-code/${code}`,
      method: 'get'
    })
  },

  /** 接受邀请 */
  accept(code: string): Promise<TenantMemberInvite> {
    return request({
      url: '/auth/tenant/invite/accept',
      method: 'post',
      data: { code }
    })
  }
}
