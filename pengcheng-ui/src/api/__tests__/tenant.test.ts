import { describe, expect, it, vi, beforeEach } from 'vitest'

// 必须在 import 被测代码前 mock
vi.mock('@/utils/request', () => {
  return {
    request: vi.fn()
  }
})

import { request } from '@/utils/request'
import { tenantApi, roleDataScopeApi } from '@/api/tenant'

const mockRequest = request as unknown as ReturnType<typeof vi.fn>

describe('tenantApi.registerTenant 端到端 mock 流程', () => {
  beforeEach(() => {
    mockRequest.mockReset()
  })

  it('企业一分钟注册：组装 POST /auth/tenant/register 并返回登录 token', async () => {
    mockRequest.mockResolvedValueOnce({
      tenantId: 1001,
      tenantCode: 'tabc12345xyz',
      adminUserId: 2002,
      defaultDeptId: 3003,
      defaultRoleId: 4004,
      login: { token: 'tk_xyz', userId: 2002, username: 'admin_demo' }
    })

    const result = await tenantApi.registerTenant({
      tenantName: 'Demo 公司',
      industry: 'realty',
      scale: '1-50',
      adminUsername: 'admin_demo',
      adminPassword: 'Pwd@12345',
      adminPhone: '13800138000'
    })

    expect(mockRequest).toHaveBeenCalledTimes(1)
    expect(mockRequest).toHaveBeenCalledWith({
      url: '/auth/tenant/register',
      method: 'post',
      data: expect.objectContaining({
        tenantName: 'Demo 公司',
        adminUsername: 'admin_demo',
        adminPassword: 'Pwd@12345'
      })
    })
    expect(result.tenantCode).toBe('tabc12345xyz')
    expect(result.login?.token).toBe('tk_xyz')
  })

  it('roleDataScopeApi.options：调用 GET /auth/role/data-scope/options', async () => {
    mockRequest.mockResolvedValueOnce([
      { value: 1, label: '全部' },
      { value: 5, label: '仅本人' }
    ])
    const opts = await roleDataScopeApi.options()
    expect(mockRequest).toHaveBeenCalledWith({
      url: '/auth/role/data-scope/options',
      method: 'get'
    })
    expect(opts).toHaveLength(2)
  })

  it('roleDataScopeApi.update：将选中的数据范围传给 PUT 接口', async () => {
    mockRequest.mockResolvedValueOnce(undefined)
    await roleDataScopeApi.update(7, 4)
    expect(mockRequest).toHaveBeenCalledWith({
      url: '/auth/role/data-scope/7',
      method: 'put',
      data: { dataScope: 4 }
    })
  })
})
