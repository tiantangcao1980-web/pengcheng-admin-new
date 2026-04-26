import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

/**
 * V4.0 MVP 闭环④ 前端单测配置（独立于 vite.config.ts，不影响业务构建）。
 * 仅覆盖 Copilot 组件库的轻量渲染测试。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    include: ['src/**/__tests__/**/*.spec.ts', 'src/**/*.spec.ts'],
    setupFiles: ['src/components/copilot/__tests__/setup.ts']
  }
})
