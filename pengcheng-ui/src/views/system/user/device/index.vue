<template>
  <div class="device-page">
    <n-page-header title="我的登录设备" subtitle="V4.0 闭环 ① 个人中心·设备管理" />

    <n-card style="margin-top: 16px">
      <template #header-extra>
        <n-button @click="load" :loading="loading">刷新</n-button>
      </template>
      <n-data-table :columns="columns" :data="devices" :loading="loading" />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted } from 'vue'
import {
  NPageHeader,
  NCard,
  NButton,
  NDataTable,
  NTag,
  NPopconfirm,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import { deviceApi, type UserLoginDevice } from '@/api/device'

const message = useMessage()
const devices = ref<UserLoginDevice[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    devices.value = await deviceApi.myDevices()
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function kickout(device: UserLoginDevice) {
  try {
    await deviceApi.kickout(device.id)
    message.success(`已下线 ${device.deviceName || device.clientType}`)
    await load()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

const STATUS_LABELS: Record<number, { label: string; type: 'default' | 'success' | 'warning' | 'error' }> = {
  0: { label: '已离线', type: 'default' },
  1: { label: '在线', type: 'success' },
  2: { label: '已踢下线', type: 'warning' }
}

const columns: DataTableColumns<UserLoginDevice> = [
  { key: 'deviceName', title: '设备', render: (row) => row.deviceName || `${row.os} / ${row.browser}` },
  { key: 'clientType', title: '客户端' },
  { key: 'ip', title: 'IP' },
  { key: 'location', title: '归属地' },
  { key: 'loginTime', title: '登录时间' },
  { key: 'lastActive', title: '最近活跃' },
  {
    key: 'status',
    title: '状态',
    render: (row) => {
      const cfg = STATUS_LABELS[row.status] ?? { label: '未知', type: 'default' as const }
      return h(NTag, { type: cfg.type, size: 'small' }, { default: () => cfg.label })
    }
  },
  {
    key: 'op',
    title: '操作',
    render: (row) => {
      if (row.status !== 1) return ''
      return h(
        NPopconfirm,
        { onPositiveClick: () => kickout(row) },
        {
          trigger: () => h(NButton, { size: 'tiny', type: 'error' }, { default: () => '踢下线' }),
          default: () => '确认要踢下线该设备？'
        }
      )
    }
  }
]

onMounted(load)
</script>

<style scoped>
.device-page {
  padding: 16px;
}
</style>
