<!--
  V4.0 MVP 闭环④ Copilot 消息气泡。
  支持：用户/助手左右气泡 + Markdown 渲染 + 工具建议确认按钮。
-->
<template>
  <div :class="['copilot-bubble', `copilot-bubble-${msg.role}`]" data-testid="copilot-bubble">
    <div class="copilot-bubble-avatar">
      <span v-if="msg.role === 'user'">我</span>
      <span v-else-if="msg.role === 'assistant'">AI</span>
      <span v-else>系统</span>
    </div>
    <div class="copilot-bubble-content">
      <StreamRenderer :text="msg.content" :streaming="msg.streaming" />

      <!-- 工具建议二次确认 -->
      <div v-if="msg.pendingAction" class="copilot-bubble-action" data-testid="copilot-action-confirm">
        <div class="copilot-action-summary">
          建议执行：{{ msg.pendingAction.summary }}
        </div>
        <div class="copilot-action-buttons">
          <button class="copilot-btn copilot-btn-primary" @click="$emit('confirm', msg)">确认执行</button>
          <button class="copilot-btn" @click="$emit('cancel', msg)">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import StreamRenderer from './StreamRenderer.vue'
import type { CopilotMessage } from './CopilotStore'

defineProps<{ msg: CopilotMessage }>()
defineEmits<{
  (e: 'confirm', m: CopilotMessage): void
  (e: 'cancel', m: CopilotMessage): void
}>()
</script>

<style scoped>
.copilot-bubble {
  display: flex;
  margin: 8px 0;
  gap: 8px;
  align-items: flex-start;
}
.copilot-bubble-user {
  flex-direction: row-reverse;
}
.copilot-bubble-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #374151;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}
.copilot-bubble-user .copilot-bubble-avatar {
  background: #111827;
  color: #fff;
}
.copilot-bubble-content {
  background: #f9fafb;
  border-radius: 12px;
  padding: 10px 14px;
  max-width: 78%;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.copilot-bubble-user .copilot-bubble-content {
  background: #111827;
  color: #fff;
}
.copilot-bubble-user .copilot-bubble-content :deep(.copilot-md) {
  color: #fff;
}
.copilot-bubble-action {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e5e7eb;
}
.copilot-action-summary {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 6px;
}
.copilot-action-buttons {
  display: flex;
  gap: 8px;
}
.copilot-btn {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 6px;
  padding: 4px 12px;
  cursor: pointer;
  font-size: 12px;
}
.copilot-btn-primary {
  background: #111827;
  color: #fff;
  border-color: #111827;
}
</style>
