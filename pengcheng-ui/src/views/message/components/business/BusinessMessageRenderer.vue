<template>
  <component :is="renderer" v-if="renderer" :payload="payload" />
  <div v-else class="unknown-msg">[未知业务消息：{{ payload?.businessType }}]</div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BusinessMessagePayload, BusinessMessageType } from './types'
import CardMessage from './CardMessage.vue'
import LocationMessage from './LocationMessage.vue'
import GoodsMessage from './GoodsMessage.vue'
import FormMessage from './FormMessage.vue'
import ChoiceMessage from './ChoiceMessage.vue'
import EventMessage from './EventMessage.vue'

const props = defineProps<{ payload: BusinessMessagePayload }>()

const RENDERER_MAP: Record<BusinessMessageType, any> = {
  CARD: CardMessage,
  LOCATION: LocationMessage,
  GOODS: GoodsMessage,
  FORM: FormMessage,
  CHOICE: ChoiceMessage,
  EVENT: EventMessage
}

const renderer = computed(() => {
  if (!props.payload || !props.payload.businessType) return null
  return RENDERER_MAP[props.payload.businessType] || null
})
</script>

<style scoped>
.unknown-msg {
  font-size: 12px;
  color: var(--n-text-color-3, #999);
  padding: 6px 10px;
  background: #fdf6ec;
  border-radius: 4px;
  display: inline-block;
}
</style>
