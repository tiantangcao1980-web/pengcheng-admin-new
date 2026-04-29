<template>
  <div class="lead-convert">
    <h2>线索转客户</h2>
    <div class="form">
      <label>线索 ID</label>
      <input v-model.number="leadId" type="number" />

      <label>已存在的客户 ID（不填则需上层 Facade 创建客户后回填）</label>
      <input v-model.number="customerId" type="number" />

      <label>备注</label>
      <input v-model="remark" />

      <button @click="submit" :disabled="busy">提交转化</button>
    </div>
    <pre v-if="result">{{ result }}</pre>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { leadApi } from '@/api/leadApi'

const leadId = ref<number>()
const customerId = ref<number>()
const remark = ref('')
const busy = ref(false)
const result = ref<any>(null)

async function submit() {
  if (!leadId.value) return
  busy.value = true
  try {
    result.value = await leadApi.convert({
      leadId: leadId.value,
      customerId: customerId.value,
      remark: remark.value
    })
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.lead-convert { padding: 16px; max-width: 480px; }
.form { display: grid; gap: 8px; }
.form input { padding: 6px 8px; }
.form button { margin-top: 12px; padding: 6px 12px; cursor: pointer; }
</style>
