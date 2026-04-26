<!--
  V4.0 MVP 闭环④ Copilot 流式 Markdown 渲染器。
  - 接收 props.text，使用 marked 渲染 Markdown
  - props.streaming=true 时在末尾追加打字光标 ▍
  - 单测会断言：随 prop 变化，innerHTML 中包含追加内容 + 光标元素
-->
<template>
  <div class="copilot-stream-renderer" data-testid="copilot-stream-renderer">
    <div class="copilot-md" v-html="rendered" />
    <span v-if="streaming" class="copilot-cursor" data-testid="copilot-cursor">▍</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'

const props = defineProps<{
  text: string
  streaming?: boolean
}>()

const rendered = computed(() => {
  // 与 src/views/ai/chat/index.vue 同款用法：marked() 直接调用为同步 string
  try {
    const result = marked(props.text || '') as unknown as string
    return result
  } catch {
    return props.text || ''
  }
})
</script>

<style scoped>
.copilot-stream-renderer {
  font-size: 14px;
  line-height: 1.6;
  color: var(--copilot-text, #1f2937);
  word-break: break-word;
}
.copilot-md :deep(p) {
  margin: 0 0 8px;
}
.copilot-md :deep(pre) {
  background: #f3f4f6;
  padding: 8px 12px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 12px;
}
.copilot-md :deep(code) {
  font-family: 'Fira Code', Consolas, monospace;
}
.copilot-cursor {
  display: inline-block;
  margin-left: 2px;
  animation: copilot-blink 1s step-end infinite;
  color: #6b7280;
}
@keyframes copilot-blink {
  50% { opacity: 0; }
}
</style>
