<template>
  <div class="invite-page">
    <n-page-header title="成员邀请管理" subtitle="V4.0 闭环 ① 多渠道邀请" />

    <n-card style="margin-top: 16px" title="新建邀请">
      <n-form inline label-placement="top">
        <n-form-item label="租户ID">
          <n-input-number v-model:value="form.tenantId" :min="1" />
        </n-form-item>
        <n-form-item label="渠道">
          <n-select v-model:value="form.channel" :options="channelOptions" style="width: 140px" />
        </n-form-item>
        <n-form-item label="手机号" v-if="form.channel === 'SMS'">
          <n-input v-model:value="form.phone" placeholder="11 位手机号" :maxlength="11" />
        </n-form-item>
        <n-form-item label="过期小时">
          <n-input-number v-model:value="form.expireHours" :min="1" :max="24 * 14" />
        </n-form-item>
        <n-form-item label-style="opacity:0">
          <n-button type="primary" @click="onCreate" :loading="creating">创建</n-button>
        </n-form-item>
      </n-form>
    </n-card>

    <n-card style="margin-top: 16px" title="批量导入（CSV）">
      <p class="hint">
        CSV 格式：<code>phone,deptName,roleCode</code>，首行可为表头。失败行将在右侧反馈。
      </p>
      <n-space>
        <n-input-number v-model:value="importTenantId" :min="1" placeholder="租户ID" />
        <n-upload
          accept=".csv"
          :default-upload="false"
          :max="1"
          :on-change="onUploadChange"
        >
          <n-button>选择 CSV</n-button>
        </n-upload>
        <n-button type="primary" :disabled="!uploadedFile" @click="onImport" :loading="importing"
          >开始导入</n-button
        >
      </n-space>

      <div v-if="importResult" class="result-card">
        <n-descriptions :column="3" size="small" bordered>
          <n-descriptions-item label="总计">{{ importResult.totalCount }}</n-descriptions-item>
          <n-descriptions-item label="成功">{{ importResult.successCount }}</n-descriptions-item>
          <n-descriptions-item label="失败">{{ importResult.failCount }}</n-descriptions-item>
        </n-descriptions>
        <n-data-table
          :columns="resultColumns"
          :data="importResult.rows"
          :pagination="{ pageSize: 10 }"
          size="small"
          style="margin-top: 12px"
        />
      </div>
    </n-card>

    <n-card style="margin-top: 16px" title="邀请列表">
      <n-space style="margin-bottom: 12px">
        <n-input-number v-model:value="listTenantId" placeholder="租户ID" />
        <n-button @click="loadList" :loading="listing">查询</n-button>
      </n-space>
      <n-data-table
        :columns="listColumns"
        :data="invites"
        :pagination="{ pageSize: 10 }"
        size="small"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h } from 'vue'
import {
  NPageHeader,
  NCard,
  NForm,
  NFormItem,
  NInputNumber,
  NSelect,
  NInput,
  NButton,
  NSpace,
  NUpload,
  NDescriptions,
  NDescriptionsItem,
  NDataTable,
  NTag,
  useMessage,
  type DataTableColumns,
  type UploadFileInfo
} from 'naive-ui'
import {
  inviteApi,
  type TenantMemberInvite,
  type InviteImportResult,
  type InviteImportRow
} from '@/api/invite'

const message = useMessage()

const channelOptions = [
  { label: '短信', value: 'SMS' },
  { label: '链接', value: 'LINK' },
  { label: '二维码', value: 'QRCODE' }
]

const form = reactive({
  tenantId: 1,
  channel: 'LINK' as 'SMS' | 'LINK' | 'QRCODE',
  phone: '',
  expireHours: 72
})
const creating = ref(false)
async function onCreate() {
  if (form.channel === 'SMS' && !/^1[3-9]\d{9}$/.test(form.phone)) {
    message.error('请输入合法手机号')
    return
  }
  creating.value = true
  try {
    const invite = await inviteApi.create({
      tenantId: form.tenantId,
      channel: form.channel,
      phone: form.phone || undefined,
      expireHours: form.expireHours
    })
    message.success(`已创建：${invite.inviteCode}`)
    if (listTenantId.value === form.tenantId) {
      await loadList()
    }
  } catch (e: any) {
    message.error(e?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

// CSV 导入
const importTenantId = ref(1)
const uploadedFile = ref<File | null>(null)
const importing = ref(false)
const importResult = ref<InviteImportResult | null>(null)

function onUploadChange({ fileList }: { fileList: UploadFileInfo[] }) {
  const f = fileList[0]?.file
  uploadedFile.value = f instanceof File ? f : null
}

async function onImport() {
  if (!uploadedFile.value) {
    message.warning('请先选择 CSV 文件')
    return
  }
  importing.value = true
  try {
    importResult.value = await inviteApi.importInvites(importTenantId.value, uploadedFile.value)
    message.success(`导入完成：成功 ${importResult.value.successCount}，失败 ${importResult.value.failCount}`)
  } catch (e: any) {
    message.error(e?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

// 列表
const listTenantId = ref(1)
const invites = ref<TenantMemberInvite[]>([])
const listing = ref(false)
async function loadList() {
  listing.value = true
  try {
    invites.value = await inviteApi.list(listTenantId.value)
  } finally {
    listing.value = false
  }
}

const STATUS_LABELS: Record<number, { label: string; type: 'default' | 'success' | 'warning' | 'error' }> = {
  0: { label: '待接受', type: 'default' },
  1: { label: '已接受', type: 'success' },
  2: { label: '已撤销', type: 'warning' },
  3: { label: '已过期', type: 'error' }
}

const listColumns: DataTableColumns<TenantMemberInvite> = [
  { key: 'inviteCode', title: '邀请码' },
  { key: 'channel', title: '渠道' },
  { key: 'phone', title: '手机号' },
  {
    key: 'status',
    title: '状态',
    render: (row) => {
      const cfg = STATUS_LABELS[row.status] ?? { label: '未知', type: 'default' as const }
      return h(NTag, { type: cfg.type, size: 'small' }, { default: () => cfg.label })
    }
  },
  { key: 'expiresAt', title: '过期时间' },
  {
    key: 'op',
    title: '操作',
    render: (row) => {
      if (row.status !== 0) return ''
      return h(
        NButton,
        {
          size: 'tiny',
          type: 'warning',
          onClick: async () => {
            await inviteApi.revoke(row.id)
            message.success('已撤销')
            await loadList()
          }
        },
        { default: () => '撤销' }
      )
    }
  }
]

const resultColumns: DataTableColumns<InviteImportRow> = [
  { key: 'lineNo', title: '行号', width: 80 },
  { key: 'phone', title: '手机号' },
  {
    key: 'success',
    title: '结果',
    render: (row) =>
      h(
        NTag,
        { type: row.success ? 'success' : 'error', size: 'small' },
        { default: () => (row.success ? '成功' : '失败') }
      )
  },
  { key: 'failReason', title: '失败原因' }
]
</script>

<style scoped>
.invite-page {
  padding: 16px;
}
.hint {
  color: #6b7280;
  font-size: 13px;
}
.hint code {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
}
.result-card {
  margin-top: 16px;
}
</style>
