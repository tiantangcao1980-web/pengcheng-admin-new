<template>
  <div class="oa-correction">
    <n-card title="补卡申请">
      <n-space vertical>
        <n-space>
          <n-button type="primary" @click="openApply">提交补卡</n-button>
          <n-select
            v-model:value="filterStatus"
            :options="[
              { label: '全部', value: undefined as any },
              { label: '待审批', value: 1 },
              { label: '已通过', value: 2 },
              { label: '已驳回', value: 3 },
            ]"
            placeholder="状态"
            clearable
            style="width: 160px"
            @update:value="loadList"
          />
          <n-button @click="loadList">刷新</n-button>
        </n-space>
        <n-data-table :columns="columns" :data="list" :loading="loading" :pagination="{ pageSize: 10 }" />
      </n-space>
    </n-card>

    <n-modal v-model:show="showApply" preset="card" title="补卡申请" style="width: 480px">
      <n-form :model="form" label-placement="left" label-width="120">
        <n-form-item label="补卡日期" required>
          <n-input v-model:value="form.correctionDate" placeholder="YYYY-MM-DD" />
        </n-form-item>
        <n-form-item label="补卡类型" required>
          <n-select
            v-model:value="form.correctionType"
            :options="[
              { label: '上班', value: 1 },
              { label: '下班', value: 2 },
            ]"
          />
        </n-form-item>
        <n-form-item label="应打卡时间" required>
          <n-input v-model:value="form.expectedTime" placeholder="YYYY-MM-DD HH:mm:ss" />
        </n-form-item>
        <n-form-item label="原因">
          <n-input v-model:value="form.reason" type="textarea" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showApply = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="submit">提交</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script lang="ts" setup>
import { h, ref } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import { oaCorrectionApi, type CorrectionItem } from '@/api/oaCorrection'

const message = useMessage()
const list = ref<CorrectionItem[]>([])
const loading = ref(false)
const showApply = ref(false)
const saving = ref(false)
const filterStatus = ref<number | undefined>()
const form = ref<CorrectionItem>(emptyForm())

function emptyForm(): CorrectionItem {
  return {
    correctionDate: '',
    correctionType: 1,
    expectedTime: '',
    reason: '',
  }
}

const statusTag = (s?: number) => {
  if (s === 1) return { type: 'warning', text: '待审批' }
  if (s === 2) return { type: 'success', text: '已通过' }
  if (s === 3) return { type: 'error', text: '已驳回' }
  return { type: 'default', text: '-' }
}

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '补卡日期', key: 'correctionDate' },
  {
    title: '类型',
    key: 'correctionType',
    render: (row: CorrectionItem) => (row.correctionType === 1 ? '上班' : '下班'),
  },
  { title: '应打卡时间', key: 'expectedTime' },
  { title: '原因', key: 'reason' },
  {
    title: '状态',
    key: 'status',
    render: (row: CorrectionItem) => {
      const t = statusTag(row.status)
      return h(NTag, { type: t.type as any }, { default: () => t.text })
    },
  },
  { title: '提交时间', key: 'createTime' },
] as any

async function loadList() {
  loading.value = true
  try {
    const r = await oaCorrectionApi.list({ status: filterStatus.value })
    list.value = (r as any) || []
  } finally {
    loading.value = false
  }
}

function openApply() {
  form.value = emptyForm()
  showApply.value = true
}

async function submit() {
  saving.value = true
  try {
    await oaCorrectionApi.submit(form.value as any)
    message.success('已提交，审批进行中')
    showApply.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

loadList()
</script>

<style scoped>
.oa-correction {
  padding: 16px;
}
</style>
