import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

/**
 * V4.0 MVP 前端单测统一配置：
 * - D1（闭环①）的 API 模块测试用 node-only 环境；
 * - D4（闭环④）的 Copilot 组件渲染测试需要 jsdom + vue 插件；
 * 这里采用 jsdom + vue plugin 的全集配置，覆盖两类用例。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    include: [
      'src/**/__tests__/**/*.{test,spec}.ts',
      'src/**/*.spec.ts'
    ],
    setupFiles: ['src/components/copilot/__tests__/setup.ts']
  }
})
