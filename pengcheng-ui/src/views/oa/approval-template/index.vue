<template>
  <div class="oa-approval-template">
    <n-card title="审批模板">
      <n-space vertical>
        <n-space>
          <n-button type="primary" @click="openCreate">新建模板</n-button>
          <n-button @click="loadList">刷新</n-button>
        </n-space>
        <n-data-table :columns="columns" :data="list" :loading="loading" :pagination="{ pageSize: 10 }" />
      </n-space>
    </n-card>

    <n-modal v-model:show="showForm" preset="card" :title="form.id ? '编辑模板' : '新建模板'" style="width: 520px">
      <n-form :model="form" label-placement="left" label-width="120">
        <n-form-item label="编码" required>
          <n-input v-model:value="form.code" placeholder="如 outing / overtime / general" :disabled="!!form.id" />
        </n-form-item>
        <n-form-item label="名称" required>
          <n-input v-model:value="form.name" />
        </n-form-item>
        <n-form-item label="分类" required>
          <n-select
            v-model:value="form.category"
            :options="[
              { label: '假勤', value: 1 },
              { label: '出差/外出', value: 2 },
              { label: '费用', value: 3 },
              { label: '通用', value: 4 },
            ]"
          />
        </n-form-item>
        <n-form-item label="默认流程ID">
          <n-input-number v-model:value="form.defaultFlowDefId" />
        </n-form-item>
        <n-form-item label="表单 schema">
          <n-input v-model:value="form.formSchema" type="textarea" :rows="6" placeholder="JSON" />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="form.remark" />
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
import { h, ref } from 'vue'
import { NButton, NPopconfirm, NTag, useMessage } from 'naive-ui'
import { oaApprovalTemplateApi, type ApprovalTemplateItem } from '@/api/oaApprovalTemplate'

const message = useMessage()
const list = ref<ApprovalTemplateItem[]>([])
const loading = ref(false)
const saving = ref(false)
const showForm = ref(false)
const form = ref<ApprovalTemplateItem>(emptyForm())

function emptyForm(): ApprovalTemplateItem {
  return {
    code: '',
    name: '',
    category: 4,
    enabled: 1,
  }
}

const categoryLabel = (v?: number) => (v === 1 ? '假勤' : v === 2 ? '出差/外出' : v === 3 ? '费用' : v === 4 ? '通用' : '-')

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '编码', key: 'code' },
  { title: '名称', key: 'name' },
  {
    title: '分类',
    key: 'category',
    render: (row: ApprovalTemplateItem) => h(NTag, {}, { default: () => categoryLabel(row.category) }),
  },
  { title: '默认流程', key: 'defaultFlowDefId' },
  {
    title: '启用',
    key: 'enabled',
    render: (row: ApprovalTemplateItem) =>
      h(NTag, { type: row.enabled === 1 ? 'success' : 'warning' }, { default: () => (row.enabled === 1 ? '是' : '否') }),
  },
  {
    title: '操作',
    key: 'actions',
    render: (row: ApprovalTemplateItem) =>
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
    const r = await oaApprovalTemplateApi.list()
    list.value = (r as any) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = emptyForm()
  showForm.value = true
}

function openEdit(row: ApprovalTemplateItem) {
  form.value = { ...row }
  showForm.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) {
      await oaApprovalTemplateApi.update(form.value)
      message.success('已更新')
    } else {
      await oaApprovalTemplateApi.create(form.value)
      message.success('已创建')
    }
    showForm.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

async function remove(row: ApprovalTemplateItem) {
  if (!row.id) return
  await oaApprovalTemplateApi.remove(row.id)
  message.success('已删除')
  await loadList()
}

loadList()
</script>

<style scoped>
.oa-approval-template {
  padding: 16px;
}
</style>
