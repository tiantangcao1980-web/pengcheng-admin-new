<!--
  V4.0 MVP H2 — Copilot Tool Call 二次确认对话框。

  当 AI 在对话流中提议执行业务动作时，前端弹出此对话框展示"将要执行什么"，
  用户确认后回调 executeAction，点取消时 emit('cancel')。

  Props:
    proposal  — 动作提议对象（来自 stream tool_proposal 事件）
  Emits:
    done(result: string)  — 用户确认且 API 返回后回调
    cancel()              — 用户取消
-->
<template>
  <n-modal
    :show="visible"
    preset="card"
    :title="dialogTitle"
    style="width: 480px; max-width: 92vw"
    :bordered="false"
    :mask-closable="false"
    @after-leave="visible = false"
  >
    <div class="tcd-body">
      <!-- 动作类型标签 -->
      <div class="tcd-action-tag">
        <n-tag :type="actionTagType" size="small">{{ actionLabel }}</n-tag>
      </div>

      <!-- 摘要 -->
      <p class="tcd-summary">{{ proposal.summary }}</p>

      <!-- 关键字段预览 -->
      <n-descriptions
        v-if="previewFields.length > 0"
        :column="1"
        label-placement="left"
        bordered
        size="small"
        class="tcd-preview"
      >
        <n-descriptions-item
          v-for="field in previewFields"
          :key="field.label"
          :label="field.label"
        >
          {{ field.value }}
        </n-descriptions-item>
      </n-descriptions>

      <!-- 提示 -->
      <p class="tcd-hint">确认后将立即执行，此操作不可自动撤销。</p>
    </div>

    <template #footer>
      <div class="tcd-footer">
        <n-button :disabled="executing" @click="onCancel">取消</n-button>
        <n-button
          type="primary"
          :loading="executing"
          @click="onConfirm"
        >
          确认执行
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { NModal, NButton, NTag, NDescriptions, NDescriptionsItem } from 'naive-ui'
import { aiCopilotApi } from '@/api/aiCopilot'

export interface ToolProposal {
  action: 'FOLLOW_UP_CREATE' | 'TODO_CREATE' | 'APPROVAL_SUBMIT'
  summary: string
  payload: Record<string, any>
  confirmToken: string
  actionId: number
}

const props = defineProps<{ proposal: ToolProposal }>()
const emit = defineEmits<{
  (e: 'done', result: string): void
  (e: 'cancel'): void
}>()

const visible = ref(true)
const executing = ref(false)

// 动作友好名称映射
const ACTION_LABEL: Record<string, string> = {
  FOLLOW_UP_CREATE: '新建跟进',
  TODO_CREATE: '创建待办',
  APPROVAL_SUBMIT: '提交审批'
}

const ACTION_TAG_TYPE: Record<string, 'info' | 'success' | 'warning'> = {
  FOLLOW_UP_CREATE: 'info',
  TODO_CREATE: 'success',
  APPROVAL_SUBMIT: 'warning'
}

const actionLabel = computed(() => ACTION_LABEL[props.proposal.action] ?? props.proposal.action)
const actionTagType = computed(() => ACTION_TAG_TYPE[props.proposal.action] ?? 'info')
const dialogTitle = computed(() => `AI 建议：${actionLabel.value}`)

/** 根据 actionCode 提取 payload 中的关键展示字段 */
const previewFields = computed<Array<{ label: string; value: string }>>(() => {
  const p = props.proposal.payload ?? {}
  const fields: Array<{ label: string; value: string }> = []

  const add = (label: string, key: string) => {
    if (p[key] != null && String(p[key]).trim() !== '') {
      fields.push({ label, value: String(p[key]) })
    }
  }

  switch (props.proposal.action) {
    case 'FOLLOW_UP_CREATE':
      add('客户', 'customerName')
      add('销售', 'salesName')
      add('跟进内容', 'note')
      add('下次跟进', 'nextFollowUpDate')
      break
    case 'TODO_CREATE':
      add('待办标题', 'title')
      add('截止时间', 'dueDate')
      add('备注', 'note')
      break
    case 'APPROVAL_SUBMIT':
      add('审批流', 'flowName')
      add('摘要', 'summary')
      add('金额', 'amount')
      break
    default:
      // 通用：展示前 4 个 key
      Object.entries(p).slice(0, 4).forEach(([k, v]) => {
        fields.push({ label: k, value: String(v) })
      })
  }

  return fields
})

async function onConfirm() {
  executing.value = true
  try {
    const result = await aiCopilotApi.executeAction({ token: props.proposal.confirmToken })
    visible.value = false
    emit('done', result ?? '动作已执行')
  } catch (err: any) {
    // 保持对话框可见，让用户看到错误（由父组件通过 done/cancel 处理）
    emit('done', `执行失败：${err?.message ?? err}`)
    visible.value = false
  } finally {
    executing.value = false
  }
}

function onCancel() {
  visible.value = false
  emit('cancel')
}
</script>

<style scoped>
.tcd-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tcd-action-tag {
  display: flex;
  align-items: center;
}

.tcd-summary {
  margin: 0;
  font-size: 14px;
  color: #111827;
  font-weight: 500;
  line-height: 1.6;
}

.tcd-preview {
  margin-top: 4px;
}

.tcd-hint {
  margin: 0;
  font-size: 12px;
  color: #9ca3af;
}

.tcd-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
