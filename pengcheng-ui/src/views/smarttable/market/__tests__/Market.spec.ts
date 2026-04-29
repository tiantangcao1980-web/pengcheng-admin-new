import { describe, it, expect, vi, beforeEach } from 'vitest'

// mock request 必须在引入被测模块之前
vi.mock('@/utils/request', () => ({
  request: vi.fn()
}))

// mock Naive UI 的 useMessage 等 hooks（组件依赖注入）
vi.mock('naive-ui', async () => {
  const actual = await vi.importActual<any>('naive-ui')
  return {
    ...actual,
    useMessage: () => ({ success: vi.fn(), error: vi.fn(), warning: vi.fn() }),
    useDialog: () => ({})
  }
})

// mock stores/user
vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    token: 'test-token',
    hasRole: () => false,
    hasPermission: () => true
  })
}))

// mock smartTable API（分享弹窗中用于拉私有模板列表）
vi.mock('@/api/smartTable', () => ({
  smartTableApi: {
    listTemplates: vi.fn().mockResolvedValue([])
  }
}))

import { request } from '@/utils/request'
import { smartTableMarketApi } from '@/api/smartTableMarket'

const mockRequest = request as unknown as ReturnType<typeof vi.fn>

// ==================== API 层单测 ====================
describe('smartTableMarketApi', () => {
  beforeEach(() => {
    mockRequest.mockReset()
  })

  it('用例1：listMarket — 调用 GET /admin/smarttable/template-market', async () => {
    mockRequest.mockResolvedValueOnce({ records: [], total: 0 })

    await smartTableMarketApi.listMarket({ sort: 'downloads', page: 1, size: 20 })

    expect(mockRequest).toHaveBeenCalledTimes(1)
    expect(mockRequest).toHaveBeenCalledWith(
      expect.objectContaining({
        url: '/admin/smarttable/template-market',
        method: 'get',
        params: expect.objectContaining({ sort: 'downloads', page: 1, size: 20 })
      })
    )
  })

  it('用例2：listMarket 切换分类 — category 参数正确透传', async () => {
    mockRequest.mockResolvedValueOnce({ records: [], total: 0 })

    await smartTableMarketApi.listMarket({ category: 'realty', sort: 'downloads', page: 1, size: 20 })

    expect(mockRequest).toHaveBeenCalledWith(
      expect.objectContaining({
        params: expect.objectContaining({ category: 'realty' })
      })
    )
  })

  it('用例3：rateTemplate — 调用 POST /{id}/rate 并携带评分参数', async () => {
    mockRequest.mockResolvedValueOnce(undefined)

    await smartTableMarketApi.rateTemplate(42, { rating: 5, review: '非常好用' })

    expect(mockRequest).toHaveBeenCalledWith(
      expect.objectContaining({
        url: '/admin/smarttable/template-market/42/rate',
        method: 'post',
        data: { rating: 5, review: '非常好用' }
      })
    )
  })

  it('用例4：downloadTemplate — 调用 POST /{id}/download', async () => {
    mockRequest.mockResolvedValueOnce(undefined)

    await smartTableMarketApi.downloadTemplate(7)

    expect(mockRequest).toHaveBeenCalledWith(
      expect.objectContaining({
        url: '/admin/smarttable/template-market/7/download',
        method: 'post'
      })
    )
  })
})
