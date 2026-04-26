/**
 * V4.0 MVP 闭环④ AI 智能助手 — Copilot 前端 API。
 *
 * 后端契约（D4 后续会在 admin-api 落地，本 ts 先约定路径，便于前端联调）：
 *   POST   /admin/ai/copilot/chat              同步对话
 *   POST   /admin/ai/copilot/chat/stream       SSE 流式
 *   POST   /admin/ai/copilot/actions/propose   提议业务动作（生成 confirmToken）
 *   POST   /admin/ai/copilot/actions/confirm   二次确认执行
 *   POST   /admin/ai/copilot/actions/cancel    取消动作
 *   POST   /admin/ai/asr/transcribe            语音转写
 */
import { request } from '@/utils/request'

export type CopilotActionCode =
  | 'FOLLOW_UP_CREATE'
  | 'TODO_CREATE'
  | 'APPROVAL_SUBMIT'

export interface CopilotContext {
  /** 当前用户 ID（从 user store 注入） */
  userId?: number | string
  /** 当前页面 path（router.currentRoute.value.fullPath） */
  pagePath?: string
  /** 可执行动作清单（前端注册后透传给后端） */
  availableActions?: CopilotActionCode[]
  /** 项目过滤（如有） */
  projectId?: number | null
}

export interface CopilotChatRequest {
  message: string
  conversationId?: string | null
  context?: CopilotContext
}

export interface CopilotChatResponse {
  content: string
  displayType?: 'text' | 'table' | 'chart' | 'card'
  conversationId?: string
  routedAgent?: string
  structuredData?: Record<string, any>
  /** 后端如果建议触发某个动作，会在 suggestion 里返回 actionCode + payload */
  suggestion?: {
    actionCode: CopilotActionCode
    payload: Record<string, any>
    summary?: string
  }
}

export interface CopilotActionProposal {
  actionId: number
  confirmToken: string
  status: 'PENDING'
  summary: string
}

export const aiCopilotApi = {
  chat(req: CopilotChatRequest): Promise<CopilotChatResponse> {
    return request<CopilotChatResponse>({
      url: '/admin/ai/copilot/chat',
      method: 'post',
      data: req
    })
  },

  /**
   * SSE 流式对话。返回 EventSource 实例，调用方负责监听 onmessage / onerror / close。
   * 注意：后端需以 GET + query 或 POST+SSE 实现，这里以兼容 EventSource 用 POST 不可，
   * 因此提供 fetch + ReadableStream 包装。
   */
  async stream(
    req: CopilotChatRequest,
    onChunk: (text: string) => void,
    onDone?: () => void,
    onError?: (err: Error) => void
  ): Promise<() => void> {
    const ctrl = new AbortController()
    try {
      const resp = await fetch('/api/admin/ai/copilot/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(req),
        signal: ctrl.signal
      })
      if (!resp.ok || !resp.body) {
        throw new Error('stream HTTP ' + resp.status)
      }
      const reader = resp.body.getReader()
      const decoder = new TextDecoder()
      ;(async () => {
        try {
          // 持续读取 SSE 数据帧
          let buf = ''
          while (true) {
            const { value, done } = await reader.read()
            if (done) break
            buf += decoder.decode(value, { stream: true })
            // SSE 标准：\n\n 分隔事件，data: 行携带载荷
            const events = buf.split('\n\n')
            buf = events.pop() || ''
            for (const evt of events) {
              const lines = evt.split('\n')
              for (const line of lines) {
                if (line.startsWith('data:')) {
                  onChunk(line.slice(5).trimStart())
                }
              }
            }
          }
          onDone?.()
        } catch (e: any) {
          if (e?.name !== 'AbortError') onError?.(e)
        }
      })()
    } catch (e: any) {
      onError?.(e)
    }
    return () => ctrl.abort()
  },

  proposeAction(req: {
    actionCode: CopilotActionCode
    conversationId?: string
    userId?: number | string
    pagePath?: string
    payload: Record<string, any>
  }): Promise<CopilotActionProposal> {
    return request<CopilotActionProposal>({
      url: '/admin/ai/copilot/actions/propose',
      method: 'post',
      data: req
    })
  },

  confirmAction(actionId: number, confirmToken: string): Promise<string> {
    return request<string>({
      url: '/admin/ai/copilot/actions/confirm',
      method: 'post',
      data: { actionId, confirmToken }
    })
  },

  cancelAction(actionId: number, confirmToken: string): Promise<void> {
    return request<void>({
      url: '/admin/ai/copilot/actions/cancel',
      method: 'post',
      data: { actionId, confirmToken }
    })
  },

  transcribe(audioUrl: string, format = 'mp3'): Promise<{ transcript: string; provider: string }> {
    return request({
      url: '/admin/ai/asr/transcribe',
      method: 'post',
      data: { audioUrl, format }
    })
  }
}
