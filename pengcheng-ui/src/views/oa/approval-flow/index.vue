<template>
  <div class="oa-approval-flow">
    <n-card title="审批流配置">
      <n-space vertical>
        <n-space>
          <n-input v-model:value="filterBizType" placeholder="按 bizType 过滤" clearable style="width: 220px" />
          <n-button type="primary" @click="openCreate">新建流程</n-button>
          <n-button @click="loadList">刷新</n-button>
        </n-space>
        <n-data-table :columns="defColumns" :data="defs" :loading="loading" :pagination="{ pageSize: 10 }" />
      </n-space>
    </n-card>

    <n-modal v-model:show="showForm" preset="card" :title="defForm.id ? '编辑流程' : '新建流程'" style="width: 720px">
      <n-form :model="defForm" label-placement="left" label-width="100">
        <n-form-item label="业务类型" required>
          <n-input v-model:value="defForm.bizType" placeholder="如 leave / outing / correction" />
        </n-form-item>
        <n-form-item label="流程名称" required>
          <n-input v-model:value="defForm.name" />
        </n-form-item>
        <n-form-item label="设为默认">
          <n-switch v-model:value="isDefaultBool" />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="defForm.remark" />
        </n-form-item>
      </n-form>

      <n-divider>节点配置（按顺序串行）</n-divider>

      <n-data-table :columns="nodeColumns" :data="nodes" :pagination="false" />
      <n-button size="small" style="margin-top: 8px" @click="addNode">+ 添加节点</n-button>

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
import { computed, h, ref, watch } from 'vue'
import { NButton, NInput, NInputNumber, NPopconfirm, NSelect, NTag, useMessage } from 'naive-ui'
import { oaApprovalFlowApi, type ApprovalFlowDef, type ApprovalFlowNode } from '@/api/oaApprovalFlow'

const message = useMessage()
const defs = ref<ApprovalFlowDef[]>([])
const loading = ref(false)
const showForm = ref(false)
const saving = ref(false)
const filterBizType = ref<string>('')

const defForm = ref<ApprovalFlowDef>(emptyDef())
const nodes = ref<ApprovalFlowNode[]>([])

function emptyDef(): ApprovalFlowDef {
  return { bizType: '', name: '', enabled: 1, isDefault: 0 }
}

const isDefaultBool = computed({
  get: () => defForm.value.isDefault === 1,
  set: (v: boolean) => (defForm.value.isDefault = v ? 1 : 0),
})

const defColumns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: 'bizType', key: 'bizType' },
  { title: '名称', key: 'name' },
  {
    title: '默认',
    key: 'isDefault',
    render: (row: ApprovalFlowDef) =>
      row.isDefault === 1 ? h(NTag, { type: 'success' }, { default: () => '默认' }) : '',
  },
  { title: '备注', key: 'remark' },
  {
    title: '操作',
    key: 'actions',
    render: (row: ApprovalFlowDef) =>
      h('div', { style: 'display:flex;gap:8px' }, [
        h(NButton, { size: 'tiny', onClick: () => openEdit(row.id!) }, { default: () => '编辑' }),
        h(
          NPopconfirm,
          { onPositiveClick: () => remove(row.id!) },
          {
            trigger: () => h(NButton, { size: 'tiny', type: 'error' }, { default: () => '删除' }),
            default: () => '确定删除？',
          },
        ),
      ]),
  },
] as any

const nodeColumns = computed(
  () =>
    [
      {
        title: '顺序',
        key: 'nodeOrder',
        width: 80,
        render: (row: ApprovalFlowNode) =>
          h(NInputNumber, {
            value: row.nodeOrder,
            min: 1,
            'onUpdate:value': (v: any) => (row.nodeOrder = v),
            style: 'width:70px',
          }),
      },
      {
        title: '节点名',
        key: 'nodeName',
        render: (row: ApprovalFlowNode) =>
          h(NInput, { value: row.nodeName, 'onUpdate:value': (v: string) => (row.nodeName = v) }),
      },
      {
        title: '类型',
        key: 'nodeType',
        render: (row: ApprovalFlowNode) =>
          h(NSelect, {
            value: row.nodeType,
            options: [
              { label: '指定用户', value: 1 },
              { label: '部门主管', value: 2 },
              { label: '角色', value: 3 },
            ],
            'onUpdate:value': (v: number) => (row.nodeType = v),
            style: 'width:120px',
          }),
      },
      {
        title: '审批人IDs',
        key: 'approverIds',
        render: (row: ApprovalFlowNode) =>
          h(NInput, {
            value: row.approverIds,
            placeholder: '逗号分隔',
            'onUpdate:value': (v: string) => (row.approverIds = v),
          }),
      },
      {
        title: '超时(小时)',
        key: 'timeoutHours',
        render: (row: ApprovalFlowNode) =>
          h(NInputNumber, {
            value: row.timeoutHours,
            min: 0,
            'onUpdate:value': (v: any) => (row.timeoutHours = v),
            style: 'width:100px',
          }),
      },
      {
        title: '超时策略',
        key: 'timeoutAction',
        render: (row: ApprovalFlowNode) =>
          h(NSelect, {
            value: row.timeoutAction,
            options: [
              { label: '通过', value: 1 },
              { label: '驳回', value: 2 },
              { label: '跳过', value: 3 },
            ],
            'onUpdate:value': (v: number) => (row.timeoutAction = v),
            style: 'width:100px',
          }),
      },
      {
        title: '操作',
        key: 'op',
        render: (row: ApprovalFlowNode, idx: number) =>
          h(
            NButton,
            { size: 'tiny', type: 'error', onClick: () => removeNode(idx) },
            { default: () => '删除' },
          ),
      },
    ] as any,
)

async function loadList() {
  loading.value = true
  try {
    const r = await oaApprovalFlowApi.defList(filterBizType.value ? { bizType: filterBizType.value } : undefined)
    defs.value = (r as any) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  defForm.value = emptyDef()
  nodes.value = []
  showForm.value = true
}

async function openEdit(id: number) {
  const r: any = await oaApprovalFlowApi.defDetail(id)
  defForm.value = r.def || emptyDef()
  nodes.value = r.nodes || []
  showForm.value = true
}

function addNode() {
  nodes.value.push({
    nodeOrder: nodes.value.length + 1,
    nodeName: '审批节点',
    nodeType: 1,
    approverIds: '',
  })
}

function removeNode(idx: number) {
  nodes.value.splice(idx, 1)
}

async function save() {
  saving.value = true
  try {
    if (defForm.value.id) {
      await oaApprovalFlowApi.defUpdate({ def: defForm.value, nodes: nodes.value })
    } else {
      await oaApprovalFlowApi.defCreate({ def: defForm.value, nodes: nodes.value })
    }
    message.success('保存成功')
    showForm.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

async function remove(id: number) {
  await oaApprovalFlowApi.defDelete(id)
  message.success('已删除')
  await loadList()
}

watch(filterBizType, () => loadList())
loadList()
</script>

<style scoped>
.oa-approval-flow {
  padding: 16px;
}
</style>
