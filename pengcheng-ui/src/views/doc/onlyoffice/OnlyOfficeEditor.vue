<template>
  <div class="onlyoffice-editor-wrap">
    <!-- 加载中 -->
    <div v-if="loading" class="oo-status oo-loading">
      <n-spin size="large" />
      <p>正在加载编辑器…</p>
    </div>

    <!-- 错误提示 -->
    <div v-else-if="errorMsg" class="oo-status oo-error">
      <n-result status="error" title="编辑器加载失败" :description="errorMsg">
        <template #footer>
          <n-button type="primary" @click="init">重新加载</n-button>
        </template>
      </n-result>
    </div>

    <!-- 编辑器挂载点 -->
    <div :id="editorId" class="oo-placeholder" :class="{ hidden: loading || !!errorMsg }" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { NSpin, NResult, NButton } from 'naive-ui'
import { getConfig } from '@/api/onlyoffice'
import { loadOnlyOfficeApi } from '@/utils/onlyofficeLoader'

// ------------------------------------------------------------------ props / emits
interface Props {
  docId: number
  mode?: 'edit' | 'view'
}

const props = withDefaults(defineProps<Props>(), { mode: 'edit' })

const emit = defineEmits<{
  (e: 'loaded'): void
  (e: 'saved'): void
  (e: 'error', msg: string): void
}>()

// ------------------------------------------------------------------ state
const loading = ref(true)
const errorMsg = ref('')
let editorInstance: any = null

/** 每个 docId 生成唯一 DOM id，避免同页多实例冲突 */
const editorId = computed(() => `onlyoffice-editor-${props.docId}`)

// ------------------------------------------------------------------ lifecycle
onMounted(() => init())

onUnmounted(() => {
  if (editorInstance) {
    try { editorInstance.destroyEditor() } catch {}
    editorInstance = null
  }
})

// 当 docId / mode 变化时重新初始化
watch(() => [props.docId, props.mode], () => {
  if (editorInstance) {
    try { editorInstance.destroyEditor() } catch {}
    editorInstance = null
  }
  init()
})

// ------------------------------------------------------------------ core
async function init() {
  loading.value = true
  errorMsg.value = ''

  try {
    // 1. 从后端拿 config + serverUrl
    const { config, serverUrl } = await getConfig(props.docId, props.mode)

    // 2. 动态加载 OnlyOffice api.js（全局缓存，只加载一次）
    const DocsAPI = await loadOnlyOfficeApi(serverUrl)

    // 3. 注入事件回调到 config
    const finalConfig = {
      ...config,
      events: {
        ...(config.events ?? {}),
        onAppReady: () => {
          loading.value = false
          emit('loaded')
        },
        onDocumentStateChange: (event: any) => {
          // event.data === false 表示文档已保存
          if (event?.data === false) emit('saved')
        },
        onError: (event: any) => {
          const msg = event?.data?.errorDescription ?? '编辑器发生未知错误'
          handleError(msg)
        }
      }
    }

    // 4. 挂载编辑器（需在 loading 显示阶段先把 placeholder 渲染进 DOM）
    //    nextTick 已隐式由 DocsAPI 构造函数处理，但需确保 DOM 节点存在
    await waitForDom(editorId.value)

    editorInstance = new DocsAPI.DocEditor(editorId.value, finalConfig)

  } catch (err: any) {
    handleError(err?.message ?? String(err))
  }
}

// ------------------------------------------------------------------ helpers
function handleError(msg: string) {
  loading.value = false
  errorMsg.value = msg
  emit('error', msg)
}

/** 轮询等待 DOM 节点出现（最多 2s） */
function waitForDom(id: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (document.getElementById(id)) { resolve(); return }
    const start = Date.now()
    const t = setInterval(() => {
      if (document.getElementById(id)) { clearInterval(t); resolve() }
      else if (Date.now() - start > 2000) { clearInterval(t); reject(new Error('editor DOM 节点未找到')) }
    }, 50)
  })
}
</script>

<style scoped>
.onlyoffice-editor-wrap {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.oo-placeholder {
  flex: 1;
  width: 100%;
  min-height: 0;
}
.oo-placeholder.hidden {
  display: none;
}
.oo-status {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #666;
}
.oo-loading p {
  margin: 0;
  font-size: 14px;
}
</style>
