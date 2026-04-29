/**
 * V4.0 MVP 闭环④ Copilot 组件库导出。
 *
 * 用法：
 *   import { CopilotFab, openCopilot } from '@/components/copilot'
 *   <CopilotFab />  // 全局挂载于 App.vue
 *   openCopilot()   // 任意页面手动唤起
 */
export { default as CopilotFab } from './CopilotFab.vue'
export { default as CopilotDrawer } from './CopilotDrawer.vue'
export { default as MessageBubble } from './MessageBubble.vue'
export { default as StreamRenderer } from './StreamRenderer.vue'

export {
  copilotState,
  pushMessage,
  resetCopilot,
  openCopilot,
  closeCopilot,
  toggleCopilot,
  type CopilotMessage,
  type CopilotState
} from './CopilotStore'
