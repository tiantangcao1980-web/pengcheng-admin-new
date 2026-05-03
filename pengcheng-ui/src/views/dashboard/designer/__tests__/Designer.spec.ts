import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'

// ─── Mock API 层 ──────────────────────────────────────────────────────────────
// 使用 vi.hoisted 让常量也被提升到 vi.mock 调用之前，避免 ReferenceError
const { mockCards, mockLayout } = vi.hoisted(() => ({
  mockCards: [
    { code: 'daily_sales', name: '今日销售额', category: '销售', suggestedChart: 'number', defaultCols: 4, defaultRows: 3 },
    { code: 'monthly_trend', name: '月度趋势', category: '销售', suggestedChart: 'line', defaultCols: 6, defaultRows: 4 },
    { code: 'deal_funnel', name: '成交漏斗', category: '客户', suggestedChart: 'funnel', defaultCols: 4, defaultRows: 4 }
  ],
  mockLayout: {
    id: 1,
    ownerType: 'user' as const,
    layoutJson: [
      { cardCode: 'daily_sales', x: 0, y: 0, w: 4, h: 3 }
    ],
    isDefault: true
  }
}))

vi.mock('@/api/dashboardCard', () => ({
  listCards: vi.fn().mockResolvedValue(mockCards),
  renderCard: vi.fn().mockResolvedValue({ value: 123456 }),
  getDefaultLayout: vi.fn().mockResolvedValue(mockLayout),
  saveLayout: vi.fn().mockResolvedValue({ ...mockLayout, id: 1 }),
  listLayouts: vi.fn().mockResolvedValue([mockLayout])
}))

vi.mock('naive-ui', async () => {
  const actual = await vi.importActual<typeof import('naive-ui')>('naive-ui')
  return {
    ...actual,
    useMessage: () => ({
      success: vi.fn(),
      error: vi.fn()
    })
  }
})

// ─── Mock Pinia user store（避免 token 检查报错） ────────────────────────────

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    token: 'test-token',
    user: { id: 1, name: 'Test' },
    roles: ['admin'],
    nickname: 'Test'
  })
}))

// ─── 动态 import（在 mock 之后）─────────────────────────────────────────────────

import * as dashboardApi from '@/api/dashboardCard'

// ─── 辅助：创建 wrapper ───────────────────────────────────────────────────────

async function createWrapper() {
  const { default: Designer } = await import('../index.vue')
  const wrapper = mount(Designer, {
    global: {
      plugins: [createPinia()],
      stubs: {
        // CardWrapper 重组件，stub 掉以避免 ECharts 副作用
        CardWrapper: {
          template: '<div class="stub-card-wrapper" />',
          props: ['meta', 'data', 'loading', 'error', 'editable'],
          emits: ['delete']
        }
      }
    }
  })
  await flushPromises()
  return wrapper
}

// ─── 测试套件 ─────────────────────────────────────────────────────────────────

describe('Dashboard Designer', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(dashboardApi.listCards).mockResolvedValue(mockCards)
    vi.mocked(dashboardApi.getDefaultLayout).mockResolvedValue(mockLayout)
    vi.mocked(dashboardApi.renderCard).mockResolvedValue({ value: 123456 })
    vi.mocked(dashboardApi.saveLayout).mockResolvedValue({ ...mockLayout, id: 1 })
  })

  // ── 用例 1：mount 时调用 listCards ─────────────────────────────────────────
  it('mount 后应调用 listCards 加载卡片市场', async () => {
    await createWrapper()
    expect(dashboardApi.listCards).toHaveBeenCalledTimes(1)
  })

  // ── 用例 2：拖入卡片后画布列表 +1 ─────────────────────────────────────────
  it('addCardToCanvas 后画布 canvasItems 长度 +1', async () => {
    const wrapper = await createWrapper()

    // 初始布局来自 mockLayout（1 个卡片）
    const initialLen = (wrapper.vm as any).canvasItems.length

    // 模拟点击卡片市场中的 monthly_trend
    const card = mockCards.find(c => c.code === 'monthly_trend')!
    ;(wrapper.vm as any).addCardToCanvas(card)
    await flushPromises()

    expect((wrapper.vm as any).canvasItems.length).toBe(initialLen + 1)
    expect((wrapper.vm as any).canvasItems.some((i: any) => i.cardCode === 'monthly_trend')).toBe(true)
  })

  // ── 用例 3：删除卡片后画布列表 -1 ─────────────────────────────────────────
  it('removeCard 后画布 canvasItems 长度 -1', async () => {
    const wrapper = await createWrapper()

    // 确保至少有一个卡片
    expect((wrapper.vm as any).canvasItems.length).toBeGreaterThan(0)
    const before = (wrapper.vm as any).canvasItems.length

    ;(wrapper.vm as any).removeCard(0)
    await flushPromises()

    expect((wrapper.vm as any).canvasItems.length).toBe(before - 1)
  })

  // ── 用例 4：saveLayout 被调用时 layoutJson 序列化正确 ─────────────────────
  it('handleSave 时 saveLayout 入参的 layoutJson 序列化正确', async () => {
    const wrapper = await createWrapper()

    // 添加一张卡片
    ;(wrapper.vm as any).addCardToCanvas(mockCards[1])
    await flushPromises()

    await (wrapper.vm as any).handleSave()
    await flushPromises()

    expect(dashboardApi.saveLayout).toHaveBeenCalledTimes(1)
    const callArg = vi.mocked(dashboardApi.saveLayout).mock.calls[0][0]

    // layoutJson 必须是数组
    expect(Array.isArray(callArg.layoutJson)).toBe(true)

    // 每个 item 必须包含 cardCode / x / y / w / h
    for (const item of callArg.layoutJson) {
      expect(item).toHaveProperty('cardCode')
      expect(item).toHaveProperty('x')
      expect(item).toHaveProperty('y')
      expect(item).toHaveProperty('w')
      expect(item).toHaveProperty('h')
    }

    // 验证可正确 JSON 序列化（不含循环引用）
    expect(() => JSON.stringify(callArg.layoutJson)).not.toThrow()
    const parsed = JSON.parse(JSON.stringify(callArg.layoutJson))
    expect(parsed.length).toBe(callArg.layoutJson.length)
  })
})
