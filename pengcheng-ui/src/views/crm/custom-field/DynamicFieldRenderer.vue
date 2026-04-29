<template>
  <div class="dyn">
    <div v-for="d in defs" :key="d.id" class="dyn__item">
      <label>{{ d.label }}<span v-if="d.required" class="req">*</span></label>

      <input v-if="d.fieldType === 'text'" v-model="model[d.fieldKey]" />

      <input v-else-if="d.fieldType === 'number'" type="number" v-model.number="model[d.fieldKey]" />

      <input v-else-if="d.fieldType === 'date'" type="date" v-model="model[d.fieldKey]" />

      <select v-else-if="d.fieldType === 'select'" v-model="model[d.fieldKey]">
        <option v-for="op in parsedOptions(d)" :key="op.value" :value="op.value">{{ op.label }}</option>
      </select>

      <div v-else-if="d.fieldType === 'multi_select'">
        <label v-for="op in parsedOptions(d)" :key="op.value" class="ck">
          <input type="checkbox" :value="op.value" v-model="model[d.fieldKey]" />
          {{ op.label }}
        </label>
      </div>

      <input v-else-if="d.fieldType === 'file'" v-model="model[d.fieldKey]" placeholder="文件 URL" />

      <em v-else>不支持类型 {{ d.fieldType }}</em>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits, watch } from 'vue'
import type { CustomFieldDef } from '@/api/customField'

const props = defineProps<{ defs: CustomFieldDef[]; modelValue: Record<string, any> }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: Record<string, any>): void }>()
const model = props.modelValue || {}

watch(model, v => emit('update:modelValue', v), { deep: true })

function parsedOptions(d: CustomFieldDef): { value: string; label: string }[] {
  if (!d.optionsJson) return []
  try {
    return JSON.parse(d.optionsJson)
  } catch {
    return []
  }
}
</script>

<style scoped>
.dyn { display: grid; gap: 8px; }
.dyn__item { display: grid; gap: 4px; }
.dyn__item label { font-size: 12px; color: #555; }
.dyn__item .req { color: #d4380d; margin-left: 2px; }
.ck { font-size: 12px; margin-right: 8px; }
</style>
