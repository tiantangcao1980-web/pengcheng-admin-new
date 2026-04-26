<!--
  V4.0 MVP 闭环④ AI Copilot 配置中心（智能提醒规则）。
  - 列表展示 ai_reminder_rule
  - 启停 + 立即触发（联调用）
-->
<template>
  <div class="copilot-config-page">
    <div class="copilot-config-header">
      <h2>AI Copilot 配置 — 智能提醒规则</h2>
      <p class="copilot-config-subtitle">
        管理每日待跟进、审批堆积、公海回收等智能提醒，命中规则后自动通过站内信 / APP 推送 / 群机器人发送。
      </p>
    </div>

    <table class="copilot-table" data-testid="copilot-rule-table">
      <thead>
        <tr>
          <th>编码</th>
          <th>名称</th>
          <th>类型</th>
          <th>触发条件</th>
          <th>渠道</th>
          <th>启用</th>
          <th>上次触发</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in rules" :key="r.id">
          <td><code>{{ r.ruleCode }}</code></td>
          <td>{{ r.ruleName }}</td>
          <td>{{ r.ruleType }}</td>
          <td>
            <span v-if="r.ruleType === 'DAILY'">CRON: {{ r.cronExpr }}</span>
            <span v-else-if="r.ruleType === 'THRESHOLD'">≥ {{ r.thresholdMin }} 分钟</span>
            <span v-else-if="r.ruleType === 'PRE_EXPIRE'">提前 {{ r.preDays }} 天</span>
          </td>
          <td>{{ r.channel }}</td>
          <td>
            <input
              type="checkbox"
              :checked="r.enabled === 1"
              @change="onToggle(r, ($event.target as HTMLInputElement).checked)"
            />
          </td>
          <td>{{ r.lastFiredAt || '—' }}</td>
          <td>
            <button class="copilot-btn-mini" @click="onFire(r)">立即触发</button>
          </td>
        </tr>
        <tr v-if="rules.length === 0">
          <td colspan="8" class="copilot-empty-row">{{ loading ? '加载中…' : '暂无规则' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { aiReminderApi, type AiReminderRule } from '@/api/aiReminder'

const rules = ref<AiReminderRule[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rules.value = await aiReminderApi.list()
  } catch (e) {
    rules.value = []
  } finally {
    loading.value = false
  }
}

async function onToggle(r: AiReminderRule, on: boolean) {
  const next = { ...r, enabled: (on ? 1 : 0) as 0 | 1 }
  await aiReminderApi.update(next)
  r.enabled = next.enabled
}

async function onFire(r: AiReminderRule) {
  const result = await aiReminderApi.fire(r.id).catch(() => ({ sent: 0 }))
  window.$message?.success?.(`已触发 ${r.ruleCode}，推送 ${result.sent} 条`)
}

onMounted(load)
</script>

<style scoped>
.copilot-config-page {
  padding: 16px 24px;
}
.copilot-config-header h2 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #111827;
}
.copilot-config-subtitle {
  margin: 0 0 16px;
  font-size: 13px;
  color: #6b7280;
}
.copilot-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.copilot-table th {
  background: #f9fafb;
  text-align: left;
  padding: 10px 12px;
  font-weight: 600;
  color: #374151;
  border-bottom: 1px solid #e5e7eb;
}
.copilot-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f3f4f6;
  color: #1f2937;
}
.copilot-empty-row {
  text-align: center;
  color: #9ca3af;
  padding: 24px 0 !important;
}
.copilot-btn-mini {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 6px;
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
}
</style>
