<template>
  <div class="oa-shift">
    <n-card title="班次模板管理">
      <n-space vertical>
        <n-space>
          <n-button type="primary" @click="openCreate">新建班次</n-button>
          <n-button @click="loadList">刷新</n-button>
        </n-space>
        <n-data-table :columns="columns" :data="list" :loading="loading" :pagination="{ pageSize: 10 }" />
      </n-space>
    </n-card>

    <n-modal v-model:show="showForm" preset="card" :title="form.id ? '编辑班次' : '新建班次'" style="width: 520px">
      <n-form :model="form" label-placement="left" label-width="120">
        <n-form-item label="班次名称" required>
          <n-input v-model:value="form.shiftName" placeholder="如：标准班 / 早班" />
        </n-form-item>
        <n-form-item label="班次类型" required>
          <n-select
            v-model:value="form.shiftType"
            :options="[
              { label: '固定班次', value: 1 },
              { label: '跨夜班次', value: 2 },
              { label: '弹性班次', value: 3 },
            ]"
          />
        </n-form-item>
        <n-form-item v-if="form.shiftType !== 3" label="上班时间">
          <n-input v-model:value="form.startTime" placeholder="HH:mm:ss" />
        </n-form-item>
        <n-form-item v-if="form.shiftType !== 3" label="下班时间">
          <n-input v-model:value="form.endTime" placeholder="HH:mm:ss" />
        </n-form-item>
        <n-form-item v-if="form.shiftType !== 3" label="迟到容忍(分)">
          <n-input-number v-model:value="form.lateGraceMinutes" :min="0" />
        </n-form-item>
        <n-form-item v-if="form.shiftType !== 3" label="早退容忍(分)">
          <n-input-number v-model:value="form.earlyGraceMinutes" :min="0" />
        </n-form-item>
        <n-form-item v-if="form.shiftType === 3" label="最低工时(分)" required>
          <n-input-number v-model:value="form.minWorkMinutes" :min="1" />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="form.remark" type="textarea" />
        </n-form-item>
        <n-form-item label="启用">
          <n-switch v-model:value="enabledBool" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showForm = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="save">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { NButton, NPopconfirm, NTag, useMessage } from 'naive-ui'
import { oaShiftApi, type ShiftItem } from '@/api/oaShift'

const message = useMessage()
const list = ref<ShiftItem[]>([])
const loading = ref(false)
const saving = ref(false)
const showForm = ref(false)
const form = ref<ShiftItem>(emptyForm())

function emptyForm(): ShiftItem {
  return {
    shiftName: '',
    shiftType: 1,
    startTime: '09:00:00',
    endTime: '18:00:00',
    lateGraceMinutes: 5,
    earlyGraceMinutes: 5,
    enabled: 1,
  }
}

const enabledBool = computed({
  get: () => form.value.enabled === 1,
  set: (v: boolean) => {
    form.value.enabled = v ? 1 : 0
  },
})

const shiftTypeLabel = (v?: number) => (v === 1 ? '固定' : v === 2 ? '跨夜' : v === 3 ? '弹性' : '-')

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '名称', key: 'shiftName' },
  {
    title: '类型',
    key: 'shiftType',
    render: (row: ShiftItem) => h(NTag, { type: 'info' }, { default: () => shiftTypeLabel(row.shiftType) }),
  },
  { title: '上班', key: 'startTime' },
  { title: '下班', key: 'endTime' },
  { title: '迟到容忍', key: 'lateGraceMinutes' },
  { title: '早退容忍', key: 'earlyGraceMinutes' },
  { title: '最低工时', key: 'minWorkMinutes' },
  {
    title: '启用',
    key: 'enabled',
    render: (row: ShiftItem) =>
      h(NTag, { type: row.enabled === 1 ? 'success' : 'warning' }, { default: () => (row.enabled === 1 ? '是' : '否') }),
  },
  {
    title: '操作',
    key: 'actions',
    render: (row: ShiftItem) =>
      h('div', { style: 'display:flex;gap:8px' }, [
        h(NButton, { size: 'tiny', onClick: () => openEdit(row) }, { default: () => '编辑' }),
        h(
          NPopconfirm,
          { onPositiveClick: () => remove(row) },
          {
            trigger: () => h(NButton, { size: 'tiny', type: 'error' }, { default: () => '删除' }),
            default: () => '确定删除？',
          },
        ),
      ]),
  },
] as any

async function loadList() {
  loading.value = true
  try {
    const r = await oaShiftApi.list()
    list.value = (r as any) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = emptyForm()
  showForm.value = true
}

function openEdit(row: ShiftItem) {
  form.value = { ...row }
  showForm.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) {
      await oaShiftApi.update(form.value)
      message.success('已更新')
    } else {
      await oaShiftApi.create(form.value)
      message.success('已创建')
    }
    showForm.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

async function remove(row: ShiftItem) {
  if (!row.id) return
  await oaShiftApi.remove(row.id)
  message.success('已删除')
  await loadList()
}

loadList()
</script>

<style scoped>
.oa-shift {
  padding: 16px;
}
</style>
