/**
 * V4.0 MVP 闭环④ Copilot 全局轻量状态管理（不引入新 store / 不污染 pinia 主仓）。
 *
 * 设计：单例 reactive 对象，随 App 生命周期存在；如需持久化或跨标签同步，
 * 后续可平滑迁移到 pinia 模块（不在 D4 范围内）。
 */
import { reactive } from 'vue'

export interface CopilotMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  /** 渲染中的内容（流式追加） */
  content: string
  /** 是否仍在流式追加 */
  streaming?: boolean
  /** 后端建议的动作（如有） */
  suggestion?: {
    actionCode: string
    payload: Record<string, any>
    summary?: string
  }
  /** propose 后的 actionId / confirmToken */
  pendingAction?: {
    actionId: number
    confirmToken: string
    summary: string
  }
  createdAt: number
}

export interface CopilotState {
  /** 抽屉是否展开 */
  drawerVisible: boolean
  /** 当前会话 ID（首次对话由后端生成） */
  conversationId: string | null
  /** 消息列表 */
  messages: CopilotMessage[]
  /** 是否正在等待响应 */
  loading: boolean
  /** 当前选中的高质量模式 */
  preferHighQuality: boolean
}

export const copilotState = reactive<CopilotState>({
  drawerVisible: false,
  conversationId: null,
  messages: [],
  loading: false,
  preferHighQuality: false
})

let counter = 0
export function newMessageId() {
  counter += 1
  return `m-${Date.now()}-${counter}`
}

export function pushMessage(msg: Omit<CopilotMessage, 'id' | 'createdAt'>): CopilotMessage {
  const m: CopilotMessage = {
    id: newMessageId(),
    createdAt: Date.now(),
    ...msg
  }
  copilotState.messages.push(m)
  return m
}

export function resetCopilot() {
  copilotState.messages = []
  copilotState.conversationId = null
  copilotState.loading = false
}

export function openCopilot() {
  copilotState.drawerVisible = true
}

export function closeCopilot() {
  copilotState.drawerVisible = false
}

export function toggleCopilot() {
  copilotState.drawerVisible = !copilotState.drawerVisible
}
