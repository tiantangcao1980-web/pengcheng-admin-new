<template>
  <div class="event-msg">
    <span v-if="data.actorName" class="actor">{{ data.actorName }}</span>
    <span class="message">{{ data.message }}</span>
    <span v-if="data.eventCode" class="code">[{{ data.eventCode }}]</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BusinessMessagePayload, EventPayloadData } from './types'

const props = defineProps<{ payload: BusinessMessagePayload }>()
const data = computed(() => props.payload.data as EventPayloadData)
</script>

<style scoped>
.event-msg {
  text-align: center;
  font-size: 12px;
  color: var(--n-text-color-3, #999);
  padding: 6px 12px;
  background: var(--n-color-modal, #f5f5f5);
  border-radius: 12px;
  display: inline-block;
  margin: 4px auto;
}

.actor {
  font-weight: 500;
  margin-right: 4px;
}

.code {
  margin-left: 6px;
  opacity: 0.5;
}
</style>
