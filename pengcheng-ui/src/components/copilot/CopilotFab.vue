<!--
  V4.0 MVP 闭环④ Copilot 全局悬浮按钮（FAB）。
  - 固定右下角 24/24
  - 点击 → 打开 CopilotDrawer
  - 隐藏在登录页 / 全屏弹窗（router meta.hideCopilotFab）
-->
<template>
  <div v-if="visible" class="copilot-fab-wrapper" data-testid="copilot-fab-wrapper">
    <button class="copilot-fab" @click="toggleCopilot" aria-label="打开 AI 助手">
      <span class="copilot-fab-icon">AI</span>
    </button>
    <CopilotDrawer />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import CopilotDrawer from './CopilotDrawer.vue'
import { toggleCopilot } from './CopilotStore'

const route = (() => {
  try { return useRoute() } catch { return null }
})()

const visible = computed(() => {
  if (!route) return true
  if (route.path === '/login' || route.path === '/register') return false
  if ((route.meta as any)?.hideCopilotFab) return false
  return true
})
</script>

<style scoped>
.copilot-fab-wrapper {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 9998;
}
.copilot-fab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #111827 0%, #374151 100%);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(17, 24, 39, 0.32);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.copilot-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.42);
}
.copilot-fab-icon {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.6px;
}
</style>
