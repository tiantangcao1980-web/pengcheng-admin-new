<template>
  <NCard size="small" class="biz-card biz-card-form" hoverable>
    <div class="title">{{ data.title || '表单' }}</div>
    <ul class="field-list">
      <li v-for="f in data.fields || []" :key="f.name">
        <span class="label">{{ f.label }}</span>
        <span v-if="f.required" class="required">*</span>
        <span class="type">[{{ f.type || 'text' }}]</span>
      </li>
    </ul>
    <NButton size="tiny" tertiary block disabled>填写表单（待实现）</NButton>
  </NCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NButton, NCard } from 'naive-ui'
import type { BusinessMessagePayload, FormPayloadData } from './types'

const props = defineProps<{ payload: BusinessMessagePayload }>()
const data = computed(() => props.payload.data as FormPayloadData)
</script>

<style scoped>
.biz-card-form {
  max-width: 280px;
  border-left: 3px solid #18a058;
}

.title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
}

.field-list {
  list-style: none;
  padding: 0;
  margin: 0 0 8px;
  font-size: 12px;
}

.field-list li {
  padding: 4px 0;
  border-bottom: 1px dashed var(--n-divider-color, #eee);
}

.label {
  color: var(--n-text-color-2, #555);
}

.required {
  color: #f56c6c;
  margin: 0 4px;
}

.type {
  color: var(--n-text-color-3, #999);
  margin-left: 6px;
}
</style>
