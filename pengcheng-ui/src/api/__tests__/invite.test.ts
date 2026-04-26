import { describe, expect, it, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  request: vi.fn()
}))

import { request } from '@/utils/request'
import { inviteApi } from '@/api/invite'

const mockRequest = request as unknown as ReturnType<typeof vi.fn>

describe('inviteApi 流程 mock', () => {
  beforeEach(() => mockRequest.mockReset())

  it('create 单条 SMS 邀请：组装 channel/phone 参数', async () => {
    mockRequest.mockResolvedValueOnce({
      id: 1,
      tenantId: 1,
      inviteCode: 'CODE-XYZ',
      channel: 'SMS',
      phone: '13800138000',
      status: 0
    })
    const r = await inviteApi.create({
      tenantId: 1,
      channel: 'SMS',
      phone: '13800138000',
      expireHours: 24
    })
    expect(mockRequest).toHaveBeenCalledWith({
      url: '/auth/tenant/invite',
      method: 'post',
      data: expect.objectContaining({ channel: 'SMS', phone: '13800138000' })
    })
    expect(r.inviteCode).toBe('CODE-XYZ')
  })

  it('importInvites：构造 multipart/form-data 上传 CSV', async () => {
    mockRequest.mockResolvedValueOnce({
      totalCount: 1,
      successCount: 1,
      failCount: 0,
      rows: [{ lineNo: 2, phone: '13800138000', success: true, inviteId: 1 }]
    })
    const file = new File(['phone\n13800138000\n'], 'invite.csv', { type: 'text/csv' })
    const result = await inviteApi.importInvites(1, file)
    expect(mockRequest).toHaveBeenCalledTimes(1)
    const callArg = mockRequest.mock.calls[0][0] as any
    expect(callArg.url).toBe('/auth/tenant/invite/import')
    expect(callArg.method).toBe('post')
    expect(callArg.data).toBeInstanceOf(FormData)
    expect(callArg.headers['Content-Type']).toBe('multipart/form-data')
    expect(result.successCount).toBe(1)
  })

  it('revoke：发送 POST 撤销请求', async () => {
    mockRequest.mockResolvedValueOnce(undefined)
    await inviteApi.revoke(99)
    expect(mockRequest).toHaveBeenCalledWith({
      url: '/auth/tenant/invite/99/revoke',
      method: 'post'
    })
  })
})
