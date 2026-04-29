<template>
  <div class="cf">
    <h2>自定义字段配置</h2>
    <div class="filter">
      <label>实体</label>
      <select v-model="entityType" @change="reload">
        <option value="lead">线索 lead</option>
        <option value="customer">客户 customer</option>
      </select>
      <button @click="creating = true">新增字段</button>
    </div>

    <table>
      <thead>
        <tr><th>排序</th><th>key</th><th>显示名</th><th>类型</th><th>必填</th><th>选项</th></tr>
      </thead>
      <tbody>
        <tr v-for="d in defs" :key="d.id">
          <td>{{ d.sortOrder }}</td>
          <td>{{ d.fieldKey }}</td>
          <td>{{ d.label }}</td>
          <td>{{ d.fieldType }}</td>
          <td>{{ d.required ? '是' : '否' }}</td>
          <td><code>{{ d.optionsJson || '-' }}</code></td>
        </tr>
      </tbody>
    </table>

    <section class="creator" v-if="creating">
      <h3>新增字段</h3>
      <input v-model="form.fieldKey" placeholder="fieldKey（英文唯一）" />
      <input v-model="form.label" placeholder="显示名" />
      <select v-model="form.fieldType">
        <option v-for="t in types" :key="t" :value="t">{{ t }}</option>
      </select>
      <label><input type="checkbox" v-model="form.required" /> 必填</label>
      <textarea v-model="form.optionsJson" placeholder='select 时填 [{"value":"a","label":"A"}]'></textarea>
      <textarea v-model="form.validationJson" placeholder='校验 JSON：{"min":1,"max":10,"pattern":"^\\d+$"}'></textarea>
      <button @click="save">保存</button>
      <button @click="creating = false">取消</button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { customFieldApi, type CustomFieldDef, type CustomFieldType } from '@/api/customField'

const types: CustomFieldType[] = ['text', 'number', 'date', 'select', 'multi_select', 'file']
const entityType = ref('lead')
const defs = ref<CustomFieldDef[]>([])
const creating = ref(false)
const form = reactive<any>({
  fieldKey: '',
  label: '',
  fieldType: 'text',
  required: false,
  optionsJson: '',
  validationJson: ''
})

async function reload() {
  const res: any = await customFieldApi.listDefs(entityType.value)
  defs.value = res?.data ?? res ?? []
}

async function save() {
  await customFieldApi.createDef({
    entityType: entityType.value,
    fieldKey: form.fieldKey,
    label: form.label,
    fieldType: form.fieldType,
    required: form.required ? 1 : 0,
    optionsJson: form.optionsJson || undefined,
    validationJson: form.validationJson || undefined
  })
  creating.value = false
  await reload()
}

onMounted(reload)
</script>

<style scoped>
.cf { padding: 16px; }
.cf table { width: 100%; border-collapse: collapse; margin-top: 12px; }
.cf th, .cf td { padding: 8px; border-bottom: 1px solid #eee; font-size: 13px; text-align: left; }
.filter { display: flex; gap: 8px; align-items: center; }
.creator { display: grid; gap: 8px; max-width: 480px; margin-top: 16px; padding: 12px; border: 1px solid #eee; border-radius: 4px; }
.creator input, .creator textarea, .creator select { padding: 6px 8px; }
</style>
