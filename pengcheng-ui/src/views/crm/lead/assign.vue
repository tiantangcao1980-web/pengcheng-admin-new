<template>
  <div class="lead-assign">
    <h2>线索批量分配</h2>
    <div class="form">
      <label>线索 ID（逗号分隔）</label>
      <input v-model="leadIdsRaw" />

      <label>分配规则</label>
      <select v-model="ruleType">
        <option value="manual">manual（指定单人）</option>
        <option value="round_robin">round_robin（轮询）</option>
        <option value="load_balance">load_balance（负载均衡）</option>
      </select>

      <template v-if="ruleType === 'manual'">
        <label>目标用户 ID</label>
        <input v-model.number="targetUserId" type="number" />
      </template>

      <template v-else>
        <label>候选用户 IDs（逗号分隔）</label>
        <input v-model="candidatesRaw" />
      </template>

      <label>备注</label>
      <input v-model="note" />

      <button @click="submit" :disabled="busy">提交分配</button>
    </div>
    <pre v-if="lastResult">{{ lastResult }}</pre>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { leadApi, type LeadAssignDTO } from '@/api/leadApi'

const leadIdsRaw = ref('')
const ruleType = ref<LeadAssignDTO['ruleType']>('manual')
const targetUserId = ref<number | undefined>(undefined)
const candidatesRaw = ref('')
const note = ref('')
const busy = ref(false)
const lastResult = ref<any>(null)

async function submit() {
  const leadIds = leadIdsRaw.value.split(',').map(s => Number(s.trim())).filter(n => !!n)
  const candidateUserIds = candidatesRaw.value.split(',').map(s => Number(s.trim())).filter(n => !!n)
  busy.value = true
  try {
    lastResult.value = await leadApi.assign({
      leadIds,
      ruleType: ruleType.value,
      targetUserId: targetUserId.value,
      candidateUserIds,
      note: note.value
    })
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.lead-assign { padding: 16px; max-width: 480px; }
.form { display: grid; gap: 8px; margin-top: 12px; }
.form label { font-size: 12px; color: #666; }
.form input, .form select { padding: 6px 8px; }
.form button { margin-top: 12px; padding: 6px 12px; cursor: pointer; }
</style>
