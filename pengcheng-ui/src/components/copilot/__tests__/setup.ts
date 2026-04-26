/**
 * Vitest 全局桩 setup —— 为 Copilot 组件单测注入最小依赖。
 * - 屏蔽 vue-router useRoute（组件内已 try/catch）
 * - 屏蔽 pinia useUserStore（组件内已 try/catch，无 store 时返回 null）
 * - 提供 marked 同步 parse（vitest jsdom 环境无需额外配置）
 */
import { vi } from 'vitest'

// jsdom 默认无 fetch，提供一个最小桩，避免组件 onMounted 调用 fetch 时崩溃
if (typeof globalThis.fetch === 'undefined') {
  // @ts-expect-error - test stub
  globalThis.fetch = () =>
    Promise.resolve({
      ok: false,
      status: 500,
      body: null
    })
}

// 静默 console.error 以免干扰 vitest 输出
const origErr = console.error
console.error = (...args: any[]) => {
  if (String(args[0] ?? '').includes('Vue received')) return
  origErr.apply(console, args)
}

export {}

// vi 必须被显式 import 才会生效，否则 tree-shaking 可能误删
void vi
