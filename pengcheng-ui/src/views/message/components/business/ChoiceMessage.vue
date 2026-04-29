<template>
  <NCard size="small" class="biz-card biz-card-choice" hoverable>
    <div class="question">{{ data.question }}</div>
    <NRadioGroup v-model:value="selected" size="small">
      <NSpace vertical :size="6">
        <NRadio
          v-for="opt in data.options || []"
          :key="opt.value"
          :value="opt.value"
        >
          {{ opt.label }}
        </NRadio>
      </NSpace>
    </NRadioGroup>
  </NCard>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { NCard, NRadio, NRadioGroup, NSpace } from 'naive-ui'
import type { BusinessMessagePayload, ChoicePayloadData } from './types'

const props = defineProps<{ payload: BusinessMessagePayload }>()
const data = computed(() => props.payload.data as ChoicePayloadData)
const selected = ref<string | number | null>(null)
</script>

<style scoped>
.biz-card-choice {
  max-width: 280px;
  border-left: 3px solid #8a2be2;
}

.question {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 10px;
}
</style>
