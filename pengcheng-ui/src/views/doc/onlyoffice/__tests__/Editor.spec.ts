import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, h } from 'vue'

// ------------------------------------------------------------------ mocks

// Mock getConfig API
vi.mock('@/api/onlyoffice', () => ({
  getConfig: vi.fn()
}))

// Mock onlyofficeLoader
vi.mock('@/utils/onlyofficeLoader', () => ({
  loadOnlyOfficeApi: vi.fn()
}))

import { getConfig } from '@/api/onlyoffice'
import { loadOnlyOfficeApi } from '@/utils/onlyofficeLoader'

// Naive-ui stubs — 避免完整渲染依赖
vi.mock('naive-ui', () => ({
  NSpin: defineComponent({ render: () => h('div', { class: 'n-spin' }) }),
  NResult: defineComponent({
    props: ['status', 'title', 'description'],
    render() { return h('div', { class: 'n-result', 'data-status': this.status }, this.description) }
  }),
  NButton: defineComponent({
    props: ['type'],
    emits: ['click'],
    render() { return h('button', { onClick: () => this.$emit('click') }, this.$slots.default?.()) }
  })
}))

// ------------------------------------------------------------------ helpers

const FAKE_SERVER_URL = 'http://onlyoffice.test'
const FAKE_CONFIG = {
  document: { key: 'doc-key-1', title: 'test.docx' },
  editorConfig: { mode: 'edit', user: { id: '1', name: 'User-1' } }
}

function makeDocsApiMock(onInit: (id: string, cfg: any) => void = () => {}) {
  return {
    DocEditor: vi.fn((id: string, cfg: any) => {
      onInit(id, cfg)
      // 立刻触发 onAppReady
      cfg?.events?.onAppReady?.()
      return { destroyEditor: vi.fn() }
    })
  }
}

// ------------------------------------------------------------------ tests

describe('OnlyOfficeEditor', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    // 确保 DOM 节点存在（jsdom 环境）
    document.body.innerHTML = ''
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ----------------------------------------------------------------
  // 用例 1：mount 后调用 getConfig
  // ----------------------------------------------------------------
  it('mount 后调用 getConfig(docId, mode)', async () => {
    const mockGetConfig = vi.mocked(getConfig)
    mockGetConfig.mockResolvedValue({ config: FAKE_CONFIG, serverUrl: FAKE_SERVER_URL })

    const mockLoad = vi.mocked(loadOnlyOfficeApi)
    mockLoad.mockResolvedValue(makeDocsApiMock() as any)

    // 动态导入组件（保证 mock 在 import 之前就位）
    const { default: OnlyOfficeEditor } = await import('../OnlyOfficeEditor.vue')

    const wrapper = mount(OnlyOfficeEditor, {
      props: { docId: 42, mode: 'edit' },
      attachTo: document.body
    })

    await flushPromises()

    expect(mockGetConfig).toHaveBeenCalledTimes(1)
    expect(mockGetConfig).toHaveBeenCalledWith(42, 'edit')
  })

  // ----------------------------------------------------------------
  // 用例 2：DocsAPI 不可用（loadOnlyOfficeApi reject）→ 显示错误提示
  // ----------------------------------------------------------------
  it('DocsAPI 不可用时显示错误状态', async () => {
    const mockGetConfig = vi.mocked(getConfig)
    mockGetConfig.mockResolvedValue({ config: FAKE_CONFIG, serverUrl: FAKE_SERVER_URL })

    const mockLoad = vi.mocked(loadOnlyOfficeApi)
    mockLoad.mockRejectedValue(new Error('[OnlyOffice] api.js 加载超时（10s）'))

    const { default: OnlyOfficeEditor } = await import('../OnlyOfficeEditor.vue')

    const wrapper = mount(OnlyOfficeEditor, {
      props: { docId: 99, mode: 'edit' },
      attachTo: document.body
    })

    await flushPromises()

    // 应渲染错误结果组件（NResult）
    const result = wrapper.find('.n-result')
    expect(result.exists()).toBe(true)
    expect(result.attributes('data-status')).toBe('error')
    // 加载中 Spin 应消失
    expect(wrapper.find('.n-spin').exists()).toBe(false)
  })

  // ----------------------------------------------------------------
  // 用例 3：mode=view 时 config.editorConfig.mode 为 'view'
  // ----------------------------------------------------------------
  it("mode='view' 时传给 DocsAPI 的 config.editorConfig.mode 为 'view'", async () => {
    const viewConfig = {
      ...FAKE_CONFIG,
      editorConfig: { ...FAKE_CONFIG.editorConfig, mode: 'view' }
    }

    const mockGetConfig = vi.mocked(getConfig)
    mockGetConfig.mockResolvedValue({ config: viewConfig, serverUrl: FAKE_SERVER_URL })

    let capturedConfig: any = null
    const mockLoad = vi.mocked(loadOnlyOfficeApi)
    mockLoad.mockResolvedValue(
      makeDocsApiMock((_id, cfg) => { capturedConfig = cfg }) as any
    )

    const { default: OnlyOfficeEditor } = await import('../OnlyOfficeEditor.vue')

    const wrapper = mount(OnlyOfficeEditor, {
      props: { docId: 7, mode: 'view' },
      attachTo: document.body
    })

    await flushPromises()

    expect(capturedConfig).not.toBeNull()
    expect(capturedConfig.editorConfig.mode).toBe('view')
  })
})
