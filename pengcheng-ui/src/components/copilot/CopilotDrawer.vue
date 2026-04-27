<!--
  V4.0 MVP 闭环④ Copilot 对话抽屉（Web 版）。
  - 与全局上下文（user/page/availableActions）一同发送给后端
  - 流式响应：边收边渲染（StreamRenderer）
  - 工具建议：弹出确认按钮 → 调用 propose/confirm
-->
<template>
  <transition name="copilot-fade">
    <div v-if="copilotState.drawerVisible" class="copilot-drawer-overlay" data-testid="copilot-drawer-overlay">
      <div class="copilot-drawer" role="dialog" aria-label="AI 助手">
        <div class="copilot-drawer-header">
          <span class="copilot-title">AI 助手</span>
          <button class="copilot-icon-btn" @click="closeCopilot">×</button>
        </div>

        <div class=”copilot-drawer-body” ref=”bodyRef”>
          <div v-if=”copilotState.messages.length === 0” class=”copilot-empty”>
            您可以这样问我：<br />
            “这个月我跟了几个客户？”<br />
            “帮我写条给王总的回访话术。”<br />
            “今天我要做什么？”
          </div>
          <MessageBubble
            v-for=”msg in copilotState.messages”
            :key=”msg.id”
            :msg=”msg”
            @confirm=”onConfirmAction”
            @cancel=”onCancelAction”
          />

          <!-- H2：AI 提议动作时弹出二次确认对话框 -->
          <ToolConfirmDialog
            v-if=”copilotState.pendingProposal != null”
            :proposal=”copilotState.pendingProposal”
            @done=”onProposalDone”
            @cancel=”onProposalCancel”
          />
        </div>

        <div class="copilot-drawer-footer">
          <textarea
            v-model="input"
            :disabled="copilotState.loading"
            class="copilot-input"
            rows="2"
            placeholder="问我任何业务相关的问题…（Enter 发送，Shift+Enter 换行）"
            @keydown="onKeydown"
          />
          <div class="copilot-footer-row">
            <label class="copilot-checkbox">
              <input type="checkbox" v-model="copilotState.preferHighQuality" />
              高质量模式
            </label>
            <button
              class="copilot-btn copilot-btn-primary"
              :disabled="!input.trim() || copilotState.loading"
              @click="onSend"
            >
              {{ copilotState.loading ? '思考中…' : '发送' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import MessageBubble from './MessageBubble.vue'
import ToolConfirmDialog from './ToolConfirmDialog.vue'
import {
  closeCopilot,
  copilotState,
  pushMessage,
  type CopilotMessage
} from './CopilotStore'
import { aiCopilotApi, type CopilotChatRequest, type CopilotContext } from '@/api/aiCopilot'
import { useUserStore } from '@/stores/user'

const route = (() => {
  try { return useRoute() } catch { return null }
})()
const userStore = (() => {
  try { return useUserStore() } catch { return null }
})()

const input = ref('')
const bodyRef = ref<HTMLElement | null>(null)
let abort: (() => void) | null = null

const ctx = computed<CopilotContext>(() => ({
  userId: (userStore?.user as any)?.id,
  pagePath: route?.fullPath ?? '/',
  availableActions: ['FOLLOW_UP_CREATE', 'TODO_CREATE', 'APPROVAL_SUBMIT']
}))

watch(
  () => copilotState.messages.length,
  () => nextTick(() => {
    if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  })
)

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    onSend()
  }
}

async function onSend() {
  const text = input.value.trim()
  if (!text || copilotState.loading) return
  input.value = ''
  pushMessage({ role: 'user', content: text })
  const assistant = pushMessage({ role: 'assistant', content: '', streaming: true })

  copilotState.loading = true
  const req: CopilotChatRequest = {
    message: text,
    conversationId: copilotState.conversationId,
    context: ctx.value
  }

  abort = await aiCopilotApi.stream(
    req,
    (chunk) => onStreamChunk(assistant, chunk),
    () => onStreamDone(assistant),
    (err) => onStreamError(assistant, err)
  )
}

function onStreamChunk(target: CopilotMessage, chunk: string) {
  // H2：检测 SSE tool_proposal 事件行（格式：event:tool_proposal\ndata:{json}）
  // fetch+ReadableStream 模式下 event type 由调用方在外层解析后传入；
  // 这里同时兼容直接在 data 帧中携带 __type=tool_proposal 的 JSON 包装格式。
  if (chunk.startsWith('{') && chunk.includes('"__type":"tool_proposal"')) {
    try {
      const ev = JSON.parse(chunk)
      if (ev.__type === 'tool_proposal') {
        copilotState.pendingProposal = {
          action: ev.action,
          summary: ev.summary,
          payload: ev.payload ?? {},
          confirmToken: ev.confirmToken,
          actionId: ev.actionId
        }
        return // 不追加到消息内容
      }
    } catch (_) { /* 解析失败则当普通文本处理 */ }
  }
  target.content += chunk
}

function onStreamDone(target: CopilotMessage) {
  target.streaming = false
  copilotState.loading = false
  // 解析最后一帧是否包含 [SUGGEST]:{json} 形式的动作建议（约定）
  const m = target.content.match(/\[SUGGEST]:(\{[\s\S]*?})\s*$/)
  if (m) {
    try {
      const sug = JSON.parse(m[1])
      target.suggestion = sug
      // 移除尾部建议标记，留 UI 展示
      target.content = target.content.replace(m[0], '').trim()
      proposeAction(target, sug)
    } catch (_) { /* ignore parse error */ }
  }
}

function onStreamError(target: CopilotMessage, err: Error) {
  target.streaming = false
  target.content += `\n\n_请求失败：${err.message}_`
  copilotState.loading = false
}

async function proposeAction(target: CopilotMessage, sug: any) {
  try {
    const proposal = await aiCopilotApi.proposeAction({
      actionCode: sug.actionCode,
      conversationId: copilotState.conversationId ?? undefined,
      userId: ctx.value.userId,
      pagePath: ctx.value.pagePath,
      payload: sug.payload || {}
    })
    target.pendingAction = {
      actionId: proposal.actionId,
      confirmToken: proposal.confirmToken,
      summary: proposal.summary
    }
  } catch (e: any) {
    target.content += `\n\n_动作提议失败：${e.message}_`
  }
}

async function onConfirmAction(msg: CopilotMessage) {
  if (!msg.pendingAction) return
  try {
    const result = await aiCopilotApi.confirmAction(
      msg.pendingAction.actionId,
      msg.pendingAction.confirmToken
    )
    msg.content += `\n\n✅ ${result || '动作已执行'}`
  } catch (e: any) {
    msg.content += `\n\n❌ 执行失败：${e.message}`
  } finally {
    msg.pendingAction = undefined
  }
}

async function onCancelAction(msg: CopilotMessage) {
  if (!msg.pendingAction) return
  await aiCopilotApi.cancelAction(msg.pendingAction.actionId, msg.pendingAction.confirmToken).catch(() => null)
  msg.pendingAction = undefined
}

/** H2：ToolConfirmDialog 确认完成（成功或失败均走此路径） */
function onProposalDone(result: string) {
  pushMessage({ role: 'assistant', content: `✅ ${result}` })
  copilotState.pendingProposal = null
}

/** H2：用户取消 ToolConfirmDialog */
function onProposalCancel() {
  copilotState.pendingProposal = null
}

onBeforeUnmount(() => {
  abort?.()
})
</script>

<style scoped>
.copilot-drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.16);
  display: flex;
  justify-content: flex-end;
}
.copilot-drawer {
  width: 420px;
  max-width: 90vw;
  background: #fff;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: -2px 0 16px rgba(0, 0, 0, 0.08);
}
.copilot-drawer-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.copilot-title {
  font-weight: 600;
  font-size: 15px;
  color: #111827;
}
.copilot-icon-btn {
  border: none;
  background: transparent;
  font-size: 20px;
  cursor: pointer;
  color: #6b7280;
}
.copilot-drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}
.copilot-empty {
  color: #9ca3af;
  font-size: 13px;
  line-height: 1.8;
  padding: 24px 4px;
}
.copilot-drawer-footer {
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
  background: #fafafa;
}
.copilot-input {
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 13px;
  resize: none;
  outline: none;
  font-family: inherit;
}
.copilot-input:focus {
  border-color: #111827;
}
.copilot-footer-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.copilot-checkbox {
  font-size: 12px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 4px;
}
.copilot-btn {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 6px;
  padding: 6px 16px;
  cursor: pointer;
  font-size: 13px;
}
.copilot-btn-primary {
  background: #111827;
  color: #fff;
  border-color: #111827;
}
.copilot-btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.copilot-fade-enter-active,
.copilot-fade-leave-active {
  transition: opacity 0.18s;
}
.copilot-fade-enter-from,
.copilot-fade-leave-to {
  opacity: 0;
}
</style>
