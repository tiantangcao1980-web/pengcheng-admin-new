<template>
  <div class="oo-page">
    <!-- 顶部工具栏 -->
    <div class="oo-toolbar">
      <n-button quaternary size="small" @click="router.back()">
        <template #icon><n-icon :component="ArrowBackOutline" /></template>
        返回
      </n-button>

      <span class="oo-doc-title">{{ docId ? `文档 #${docId}` : '在线编辑' }}</span>

      <n-button-group size="small">
        <n-button :type="mode === 'edit' ? 'primary' : 'default'" @click="setMode('edit')">
          <template #icon><n-icon :component="CreateOutline" /></template>
          编辑
        </n-button>
        <n-button :type="mode === 'view' ? 'primary' : 'default'" @click="setMode('view')">
          <template #icon><n-icon :component="EyeOutline" /></template>
          预览
        </n-button>
      </n-button-group>

      <n-tag v-if="status === 'loaded'" type="success" size="small" round>已就绪</n-tag>
      <n-tag v-else-if="status === 'saved'" type="info" size="small" round>已保存</n-tag>
      <n-tag v-else-if="status === 'error'" type="error" size="small" round>加载失败</n-tag>
    </div>

    <!-- 编辑器区域 -->
    <div class="oo-body">
      <OnlyOfficeEditor
        v-if="docId"
        :docId="docId"
        :mode="mode"
        @loaded="status = 'loaded'"
        @saved="status = 'saved'"
        @error="onError"
      />
      <n-result v-else status="warning" title="参数缺失" description="未传入文档 ID，无法打开编辑器。" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NButtonGroup, NIcon, NTag, NResult, useMessage } from 'naive-ui'
import { ArrowBackOutline, CreateOutline, EyeOutline } from '@vicons/ionicons5'
import OnlyOfficeEditor from './OnlyOfficeEditor.vue'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const docId = computed(() => {
  const raw = route.params.docId
  const n = Number(Array.isArray(raw) ? raw[0] : raw)
  return Number.isFinite(n) && n > 0 ? n : 0
})

const mode = ref<'edit' | 'view'>('edit')
const status = ref<'idle' | 'loaded' | 'saved' | 'error'>('idle')

function setMode(m: 'edit' | 'view') {
  if (mode.value !== m) {
    status.value = 'idle'
    mode.value = m
  }
}

function onError(msg: string) {
  status.value = 'error'
  message.error(`编辑器错误：${msg}`)
}
</script>

<style scoped>
.oo-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}
.oo-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  border-bottom: 1px solid var(--n-border-color, #e0e0e6);
  background: var(--n-card-color, #fff);
  flex-shrink: 0;
}
.oo-doc-title {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.oo-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>
