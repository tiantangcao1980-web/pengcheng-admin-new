import { createI18n } from 'vue-i18n'
import zhCN from '@/locales/zh-CN'
import enUS from '@/locales/en-US'
import { getLocaleMessages } from '@/api/i18n'

/** 支持的 locale 列表 */
export const SUPPORTED_LOCALES = ['zh-CN', 'en-US'] as const
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]

/** 默认 locale */
const DEFAULT_LOCALE: SupportedLocale = 'zh-CN'
/** 兜底 locale（key 缺失时回退） */
const FALLBACK_LOCALE: SupportedLocale = 'en-US'

/**
 * vue-i18n 实例（legacy: false — Composition API 模式）。
 *
 * <p>启动流程：
 * 1. 先加载本地静态词条（保证首屏不��烁）；
 * 2. {@link initI18n} 被 main.ts ��用，拉取服务端词条并合并（服务端词条优先）。
 */
const i18n = createI18n({
  legacy: false,
  locale: DEFAULT_LOCALE,
  fallbackLocale: FALLBACK_LOCALE,
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
  // 关闭"找不到 key"的警告日志（生产环境）
  missingWarn: import.meta.env.DEV,
  fallbackWarn: import.meta.env.DEV,
})

/**
 * 初始化 i18n：拉取服务端 zh-CN 词条并合并到本地静态词条。
 * 由 main.ts 在 app.mount 之前调用（非阻塞；失败时静默降级）。
 */
export async function initI18n(): Promise<void> {
  await loadServerMessages(DEFAULT_LOCALE)
}

/**
 * 切换语言。
 * 1. 拉取目标 locale 的服务端词条；
 * 2. 合并到 i18n messages；
 * 3. 切换 i18n.global.locale。
 *
 * @param locale 目标 locale，如 'zh-CN' 或 'en-US'
 */
export async function setLocale(locale: SupportedLocale): Promise<void> {
  await loadServerMessages(locale)
  ;(i18n.global.locale as any).value = locale
}

/**
 * 从服务端拉取 locale 词条并以深合并方式追加（服务端 > 本地静态）。
 * 失败时静默降级，不影响已有词条。
 */
async function loadServerMessages(locale: SupportedLocale): Promise<void> {
  try {
    const serverMessages = await getLocaleMessages(locale)
    if (serverMessages && typeof serverMessages === 'object') {
      // 深合并：服务端扁平 key（如 "biz.customer.name"）覆盖本地
      const merged = deepMergeFlat(
        i18n.global.getLocaleMessage(locale) as Record<string, any>,
        serverMessages,
      )
      i18n.global.setLocaleMessage(locale, merged)
    }
  } catch {
    // 服务端不可用（离线/未配置）时静默降级到本地静态词条
    if (import.meta.env.DEV) {
      console.warn(`[i18n] 服务端词条加载失败 locale=${locale}，使用本地静态词条`)
    }
  }
}

/**
 * 将服务端扁平词条（"a.b.c" → value）深度合并到已有 messages 对象。
 * 扁平 key 展开后写入，嵌套 key 优先于扁平 key。
 */
function deepMergeFlat(
  base: Record<string, any>,
  flat: Record<string, string>,
): Record<string, any> {
  const result = { ...base }
  for (const [key, value] of Object.entries(flat)) {
    const parts = key.split('.')
    if (parts.length === 1) {
      result[key] = value
    } else {
      let node = result
      for (let i = 0; i < parts.length - 1; i++) {
        if (!node[parts[i]] || typeof node[parts[i]] !== 'object') {
          node[parts[i]] = {}
        }
        node = node[parts[i]]
      }
      node[parts[parts.length - 1]] = value
    }
  }
  return result
}

export default i18n
