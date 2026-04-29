<template>
  <NCard size="small" class="biz-card biz-card-customer" hoverable>
    <div class="card-row">
      <NAvatar round :size="40" class="avatar">
        {{ (data.customerName || '?').slice(0, 1) }}
      </NAvatar>
      <div class="info">
        <div class="name">{{ data.customerName || '未命名客户' }}</div>
        <div class="phone">{{ data.phoneMasked || '电话已脱敏' }}</div>
      </div>
    </div>
    <div v-if="data.dealProbability != null" class="probability">
      成交概率：{{ data.dealProbability }}%
    </div>
  </NCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NAvatar, NCard } from 'naive-ui'
import type { BusinessMessagePayload, CardPayloadData } from './types'

const props = defineProps<{ payload: BusinessMessagePayload }>()
const data = computed(() => props.payload.data as CardPayloadData)
</script>

<style scoped>
.biz-card-customer {
  max-width: 280px;
  border-left: 3px solid #2080f0;
}

.card-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  background: #2080f0;
  color: #fff;
}

.info {
  display: flex;
  flex-direction: column;
}

.name {
  font-weight: 600;
  font-size: 14px;
}

.phone {
  font-size: 12px;
  color: var(--n-text-color-3, #999);
}

.probability {
  margin-top: 8px;
  font-size: 12px;
  color: #67c23a;
}
</style>
