import { request } from '@/utils/request'

// V4.0 闭环 ① 租户/企业一分钟开通
export interface TenantRegisterParams {
  tenantName: string
  industry?: string
  scale?: string
  adminUsername: string
  adminPassword: string
  adminNickname?: string
  adminPhone?: string
  adminEmail?: string
  smsCode?: string
}

export interface TenantRegisterResponse {
  tenantId: number
  tenantCode: string
  adminUserId: number
  defaultDeptId: number
  defaultRoleId: number
  login?: { token: string; userId: number; username: string }
}

export const tenantApi = {
  // 企业一分钟注册
  registerTenant(data: TenantRegisterParams): Promise<TenantRegisterResponse> {
    return request({
      url: '/auth/tenant/register',
      method: 'post',
      data
    })
  }
}

// 角色数据范围
export interface DataScopeOption {
  value: number
  label: string
}

export const roleDataScopeApi = {
  options(): Promise<DataScopeOption[]> {
    return request({
      url: '/auth/role/data-scope/options',
      method: 'get'
    })
  },
  update(roleId: number, dataScope: number): Promise<void> {
    return request({
      url: `/auth/role/data-scope/${roleId}`,
      method: 'put',
      data: { dataScope }
    })
  }
}
