<template>
  <div class="cfp">
    <!-- loading 态 -->
    <div v-if="loading" class="cfp__loading">加载中...</div>

    <!-- 空态：无字段定义 -->
    <div v-else-if="defs.length === 0" class="cfp__empty">暂无自定义字段</div>

    <!-- 字段列表 -->
    <template v-else>
      <DynamicFieldRenderer :defs="defs" v-model="fieldValues" />

      <!-- 校验错误 -->
      <ul v-if="errors.length > 0" class="cfp__errors">
        <li v-for="e in errors" :key="e" class="cfp__error-item">{{ e }}</li>
      </ul>

      <!-- 保存按钮（非只读） -->
      <div v-if="!readonly" class="cfp__footer">
        <button class="cfp__save-btn" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import DynamicFieldRenderer from '@/views/crm/custom-field/DynamicFieldRenderer.vue'
import { customFieldApi, type CustomFieldDef } from '@/api/customField'

const props = withDefaults(
  defineProps<{
    entityType: 'customer' | 'lead' | 'opportunity'
    entityId: number | string
    readonly?: boolean
  }>(),
  { readonly: false }
)

const loading = ref(false)
const saving = ref(false)
const defs = ref<CustomFieldDef[]>([])
const fieldValues = ref<Record<string, any>>({})
const errors = ref<string[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const [defRes, valRes]: [any, any] = await Promise.all([
      customFieldApi.listDefs(props.entityType),
      customFieldApi.loadValues(props.entityType, Number(props.entityId))
    ])
    defs.value = defRes?.data ?? defRes ?? []
    fieldValues.value = valRes?.data ?? valRes ?? {}
  } finally {
    loading.value = false
  }
})

function validate(): boolean {
  const errs: string[] = []
  for (const def of defs.value) {
    if (def.required === 1) {
      const val = fieldValues.value[def.fieldKey]
      const empty =
        val === undefined ||
        val === null ||
        val === '' ||
        (Array.isArray(val) && val.length === 0)
      if (empty) errs.push(`"${def.label}" 为必填项`)
    }
    // 数字范围校验
    if (def.fieldType === 'number' && def.validationJson) {
      try {
        const v = JSON.parse(def.validationJson)
        const num = Number(fieldValues.value[def.fieldKey])
        if (v.min !== undefined && num < v.min) errs.push(`"${def.label}" 不能小于 ${v.min}`)
        if (v.max !== undefined && num > v.max) errs.push(`"${def.label}" 不能大于 ${v.max}`)
      } catch { /* noop */ }
    }
  }
  errors.value = errs
  return errs.length === 0
}

async function handleSave() {
  if (!validate()) return
  saving.value = true
  try {
    await customFieldApi.saveValues(props.entityType, Number(props.entityId), fieldValues.value)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.cfp { padding: 12px 0; }
.cfp__loading,
.cfp__empty {
  color: #999;
  font-size: 13px;
  padding: 16px 0;
  text-align: center;
}
.cfp__errors {
  list-style: none;
  margin: 8px 0 0;
  padding: 8px 12px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
}
.cfp__error-item { font-size: 12px; color: #cf1322; padding: 2px 0; }
.cfp__footer { margin-top: 16px; display: flex; justify-content: flex-end; }
.cfp__save-btn {
  padding: 6px 20px;
  background: #1677ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.cfp__save-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
