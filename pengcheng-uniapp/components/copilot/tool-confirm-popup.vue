<!--
  V4.0 MVP H2 — uniapp Copilot Tool Call 二次确认弹窗。

  当 AI 提议执行业务动作时，从页面底部弹出，展示动作摘要和 payload 预览，
  用户确认后调 copilotApi.executeAction(token)，点取消关闭弹窗。

  Props:
    proposal  {Object}  — 动作提议（action/summary/payload/confirmToken/actionId）
    show      {Boolean} — 控制弹窗展示
  Emits:
    done(result)  — 执行完成（成功或失败均回调）
    cancel        — 用户取消
-->
<template>
  <u-popup
    :show="show"
    mode="bottom"
    :round="20"
    :safe-area-inset-bottom="true"
    @close="onCancel"
  >
    <view class="tcp-container">
      <!-- 标题栏 -->
      <view class="tcp-header">
        <view class="tcp-drag-bar" />
        <text class="tcp-title">AI 建议：{{ actionLabel }}</text>
      </view>

      <!-- 动作类型标签 -->
      <view class="tcp-tag-row">
        <view :class="['tcp-tag', `tcp-tag-${proposal.action.toLowerCase()}`]">
          <text class="tcp-tag-text">{{ actionLabel }}</text>
        </view>
      </view>

      <!-- 摘要 -->
      <text class="tcp-summary">{{ proposal.summary }}</text>

      <!-- payload 预览 -->
      <view v-if="previewFields.length > 0" class="tcp-preview">
        <view v-for="field in previewFields" :key="field.label" class="tcp-preview-row">
          <text class="tcp-label">{{ field.label }}</text>
          <text class="tcp-value">{{ field.value }}</text>
        </view>
      </view>

      <!-- 提示 -->
      <text class="tcp-hint">确认后将立即执行，此操作不可自动撤销。</text>

      <!-- 按钮区 -->
      <view class="tcp-footer">
        <button class="tcp-btn tcp-btn-cancel" :disabled="executing" @tap="onCancel">取消</button>
        <button class="tcp-btn tcp-btn-confirm" :loading="executing" :disabled="executing" @tap="onConfirm">
          {{ executing ? '执行中…' : '确认执行' }}
        </button>
      </view>
    </view>
  </u-popup>
</template>

<script>
import { aiCopilotApi } from './copilotApi.js'

const ACTION_LABEL = {
  FOLLOW_UP_CREATE: '新建跟进',
  TODO_CREATE: '创建待办',
  APPROVAL_SUBMIT: '提交审批'
}

export default {
  name: 'ToolConfirmPopup',
  props: {
    proposal: { type: Object, required: true },
    show: { type: Boolean, default: false }
  },
  emits: ['done', 'cancel'],
  data() {
    return { executing: false }
  },
  computed: {
    actionLabel() {
      return ACTION_LABEL[this.proposal?.action] ?? this.proposal?.action ?? '执行动作'
    },
    previewFields() {
      const p = this.proposal?.payload ?? {}
      const fields = []
      const add = (label, key) => {
        if (p[key] != null && String(p[key]).trim() !== '') {
          fields.push({ label, value: String(p[key]) })
        }
      }
      switch (this.proposal?.action) {
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
          Object.entries(p).slice(0, 4).forEach(([k, v]) => {
            fields.push({ label: k, value: String(v) })
          })
      }
      return fields
    }
  },
  methods: {
    async onConfirm() {
      if (this.executing) return
      this.executing = true
      try {
        const result = await aiCopilotApi.executeAction(this.proposal.confirmToken)
        this.$emit('done', result ?? '动作已执行')
      } catch (e) {
        this.$emit('done', `执行失败：${e?.message ?? e}`)
      } finally {
        this.executing = false
      }
    },
    onCancel() {
      if (this.executing) return
      this.$emit('cancel')
    }
  }
}
</script>

<style scoped>
.tcp-container {
  padding: 0 32rpx 48rpx;
  background: #fff;
}

.tcp-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx 0 20rpx;
}

.tcp-drag-bar {
  width: 80rpx;
  height: 8rpx;
  border-radius: 4rpx;
  background: #e5e7eb;
  margin-bottom: 24rpx;
}

.tcp-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #111827;
}

.tcp-tag-row {
  margin: 8rpx 0 16rpx;
}

.tcp-tag {
  display: inline-flex;
  padding: 4rpx 20rpx;
  border-radius: 20rpx;
  background: #e0f2fe;
}

.tcp-tag-approval_submit {
  background: #fef9c3;
}

.tcp-tag-todo_create {
  background: #dcfce7;
}

.tcp-tag-text {
  font-size: 24rpx;
  color: #1e40af;
}

.tcp-summary {
  display: block;
  font-size: 30rpx;
  font-weight: 500;
  color: #1f2937;
  line-height: 1.6;
  margin-bottom: 20rpx;
}

.tcp-preview {
  background: #f9fafb;
  border-radius: 16rpx;
  padding: 16rpx 24rpx;
  margin-bottom: 20rpx;
}

.tcp-preview-row {
  display: flex;
  gap: 16rpx;
  padding: 8rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}

.tcp-preview-row:last-child {
  border-bottom: none;
}

.tcp-label {
  flex-shrink: 0;
  font-size: 26rpx;
  color: #6b7280;
  width: 160rpx;
}

.tcp-value {
  font-size: 26rpx;
  color: #111827;
  flex: 1;
  word-break: break-all;
}

.tcp-hint {
  display: block;
  font-size: 24rpx;
  color: #9ca3af;
  margin-bottom: 32rpx;
}

.tcp-footer {
  display: flex;
  gap: 24rpx;
}

.tcp-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  border: none;
}

.tcp-btn-cancel {
  background: #f3f4f6;
  color: #374151;
}

.tcp-btn-confirm {
  background: #111827;
  color: #fff;
}

.tcp-btn[disabled] {
  opacity: 0.5;
}
</style>
