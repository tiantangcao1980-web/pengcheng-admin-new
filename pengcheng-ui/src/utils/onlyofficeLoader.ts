/**
 * OnlyOffice api.js 动态加载器。
 *
 * 设计要点：
 * - 每个 serverUrl 只加载一次（Promise 缓存在 Map 中）；
 * - 已注入 <script> 且 window.DocsAPI 已存在时直接 resolve；
 * - 10 s 超时未触发 onload → reject；
 * - 加载失败（onerror）立即 reject 并从缓存移除，允许重试。
 */

declare global {
  interface Window {
    DocsAPI?: any
  }
}

const cache = new Map<string, Promise<typeof window.DocsAPI>>()

const TIMEOUT_MS = 10_000

export function loadOnlyOfficeApi(serverUrl: string): Promise<typeof window.DocsAPI> {
  // 规范化 serverUrl，去掉末尾斜杠
  const base = serverUrl.replace(/\/+$/, '')
  const scriptSrc = `${base}/web-apps/apps/api/documents/api.js`

  // 已缓存（含进行中的 Promise）
  if (cache.has(base)) {
    return cache.get(base)!
  }

  const promise = new Promise<typeof window.DocsAPI>((resolve, reject) => {
    // 已存在 DocsAPI 对象（比如 HMR 场景）
    if (window.DocsAPI) {
      resolve(window.DocsAPI)
      return
    }

    // 防止重复注入相同 src 的 script 标签
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${scriptSrc}"]`)
    if (existing) {
      // 标签存在但 DocsAPI 还未挂上，轮询等待
      const start = Date.now()
      const poll = setInterval(() => {
        if (window.DocsAPI) {
          clearInterval(poll)
          resolve(window.DocsAPI)
        } else if (Date.now() - start > TIMEOUT_MS) {
          clearInterval(poll)
          cache.delete(base)
          reject(new Error('[OnlyOffice] DocsAPI 加载超时'))
        }
      }, 200)
      return
    }

    const script = document.createElement('script')
    script.src = scriptSrc
    script.async = true

    const timer = setTimeout(() => {
      cache.delete(base)
      reject(new Error('[OnlyOffice] api.js 加载超时（10s）'))
    }, TIMEOUT_MS)

    script.onload = () => {
      clearTimeout(timer)
      if (window.DocsAPI) {
        resolve(window.DocsAPI)
      } else {
        cache.delete(base)
        reject(new Error('[OnlyOffice] api.js 已加载但 DocsAPI 未挂载'))
      }
    }

    script.onerror = () => {
      clearTimeout(timer)
      cache.delete(base)
      reject(new Error(`[OnlyOffice] 加载 api.js 失败：${scriptSrc}`))
    }

    document.head.appendChild(script)
  })

  cache.set(base, promise)
  return promise
}
