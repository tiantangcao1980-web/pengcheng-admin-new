<template>
  <NSpace vertical :size="16">
    <NPageHeader title="工单管理" subtitle="IT / HR / 财务等内部工单流转" />

    <NCard size="small">
      <template #header-extra>
        <NButton type="primary" size="small" @click="reload" :loading="loading">
          刷新
        </NButton>
      </template>

      <NDataTable
        :columns="columns"
        :data="tickets"
        :loading="loading"
        :bordered="false"
        size="small"
      />
    </NCard>

    <NAlert type="info" title="即将上线" :show-icon="false">
      工单创建 / 分配 / 回复 / 解决 / 关闭等完整状态机能力会在 V1.0 收尾完成。
      当前后端 Service 已就绪（<code>TicketService</code>），等待 Controller 接入。
    </NAlert>
  </NSpace>
</template>

<script setup lang="ts">
import { h, onMounted, ref } from 'vue'
import {
  NAlert,
  NButton,
  NCard,
  NDataTable,
  NPageHeader,
  NSpace,
  NTag,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import {
  realtyApi,
  TICKET_STATUS_LABEL,
  type TicketRecord,
  type TicketStatus
} from '@/api/realty'

const message = useMessage()
const tickets = ref<TicketRecord[]>([])
const loading = ref(false)

const STATUS_TYPE: Record<TicketStatus, 'default' | 'info' | 'warning' | 'success' | 'error'> = {
  CREATED: 'default',
  ASSIGNED: 'info',
  IN_PROGRESS: 'warning',
  RESOLVED: 'success',
  CLOSED: 'default',
  CANCELLED: 'error'
}

const columns: DataTableColumns<TicketRecord> = [
  { title: '工单号', key: 'ticketNo', width: 160 },
  { title: '标题', key: 'title' },
  { title: '类型', key: 'category', width: 80 },
  {
    title: '优先级',
    key: 'priority',
    width: 80,
    render: row => ['', '低', '中', '高', '紧急'][row.priority] || '中'
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => {
      const s = row.status as TicketStatus
      return h(NTag, { size: 'small', type: STATUS_TYPE[s] || 'default' }, {
        default: () => TICKET_STATUS_LABEL[s] || row.status
      })
    }
  },
  { title: '创建时间', key: 'createTime', width: 160 }
]

async function reload() {
  loading.value = true
  try {
    tickets.value = await realtyApi.ticketMyOpen()
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>
