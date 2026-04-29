import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock 依赖的 axios request 模块，避免真实 HTTP 请求
vi.mock('@/utils/request', () => ({
  request: vi.fn(),
}))

vi.mock('@/api/i18n', () => ({
  getLocaleMessages: vi.fn(),
}))

import { getLocaleMessages } from '@/api/i18n'
import { request } from '@/utils/request'

describe('i18n 模块', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  // ========== 用例 1：默认 locale 为 zh-CN ==========

  it('默认 locale 应为 zh-CN', async () => {
    // 模拟服务端返回空词条（不影响本地静态词条）
    vi.mocked(getLocaleMessages).mockResolvedValue({})

    // 动态 import 以获得干净的模块状态
    const { default: i18n } = await import('../index')

    expect((i18n.global.locale as any).value).toBe('zh-CN')
  })

  // ========== 用例 2：setLocale 切换后拉取新 locale 词条 ==========

  it('setLocale("en-US") 应拉取 en-US 词条并切换 locale', async () => {
    vi.mocked(getLocaleMessages).mockResolvedValue({ 'common.save': 'Save' })

    const { default: i18n, setLocale } = await import('../index')

    await setLocale('en-US')

    expect(vi.mocked(getLocaleMessages)).toHaveBeenCalledWith('en-US')
    expect((i18n.global.locale as any).value).toBe('en-US')

    // 服务端词条应已合并：common.save 覆盖本地
    const msg = i18n.global.getLocaleMessage('en-US') as any
    expect(msg?.common?.save).toBe('Save')
  })

  // ========== 用例 3：服务端请求失败时降级到本地静态词条 ==========

  it('服务端词条加载失败时应静默降级，本地静态词条仍可用', async () => {
    vi.mocked(getLocaleMessages).mockRejectedValue(new Error('network error'))

    const { default: i18n, initI18n } = await import('../index')

    // initI18n 不应抛出异常
    await expect(initI18n()).resolves.toBeUndefined()

    // 本地静态词条仍然存在
    const msg = i18n.global.getLocaleMessage('zh-CN') as any
    expect(msg?.common?.login).toBe('登录')
  })
})
