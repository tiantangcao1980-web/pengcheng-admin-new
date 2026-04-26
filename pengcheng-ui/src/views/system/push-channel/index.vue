<template>
  <div class="push-channel-page">
    <div class="page-header">
      <div>
        <h2>消息推送通道</h2>
        <p class="desc">三通道（APP 推送 / 小程序订阅消息 / Web 站内信）统一调度配置与日志</p>
      </div>
    </div>

    <n-tabs v-model:value="activeTab" type="line" animated>
      <!-- ============ 订阅消息模板 ============ -->
      <n-tab-pane name="templates" tab="订阅消息模板">
        <n-card>
          <template #header-extra>
            <n-button type="primary" size="small" @click="openTemplateForm()">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              新增模板
            </n-button>
          </template>
          <n-data-table
            :columns="templateColumns"
            :data="templates"
            :loading="templatesLoading"
            :bordered="false"
          />
        </n-card>
      </n-tab-pane>

      <!-- ============ 通道日志 ============ -->
      <n-tab-pane name="logs" tab="通道日志">
        <n-card>
          <n-form inline :model="logQuery" class="filter-form">
            <n-form-item label="通道">
              <n-select
                v-model:value="logQuery.channel"
                :options="channelOptions"
                style="width: 160px"
                clearable
              />
            </n-form-item>
            <n-form-item label="业务类型">
              <n-input v-model:value="logQuery.bizType" style="width: 160px" clearable />
            </n-form-item>
            <n-form-item label="状态">
              <n-select
                v-model:value="logQuery.success"
                :options="successOptions"
                style="width: 120px"
                clearable
              />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" @click="loadLogs">查询</n-button>
              <n-button @click="resetLogQuery">重置</n-button>
            </n-form-item>
          </n-form>

          <n-data-table
            :columns="logColumns"
            :data="logs"
            :loading="logsLoading"
            :pagination="logPagination"
          />
        </n-card>
      </n-tab-pane>

      <!-- ============ 通道测试 ============ -->
      <n-tab-pane name="test" tab="通道测试">
        <n-card>
          <n-form
            ref="testFormRef"
            :model="testForm"
            label-placement="left"
            label-width="120"
            require-mark-placement="right-hanging"
            style="max-width: 600px"
          >
            <n-form-item label="目标用户 ID" path="userId">
              <n-input-number v-model:value="testForm.userId" style="width: 100%" />
            </n-form-item>
            <n-form-item label="业务类型" path="bizType">
              <n-input v-model:value="testForm.bizType" placeholder="如 approval" />
            </n-form-item>
            <n-form-item label="业务 ID">
              <n-input-number v-model:value="testForm.bizId" style="width: 100%" />
            </n-form-item>
            <n-form-item label="标题" path="title">
              <n-input v-model:value="testForm.title" />
            </n-form-item>
            <n-form-item label="内容" path="content">
              <n-input v-model:value="testForm.content" type="textarea" />
            </n-form-item>
            <n-form-item label="订阅模板 ID">
              <n-input v-model:value="testForm.subscribeTemplateId" />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" :loading="testing" @click="runTest">发送测试</n-button>
              <n-tag v-if="lastTestResult" :type="lastTestResult.success ? 'success' : 'error'" style="margin-left: 12px">
                {{ formatChannel(lastTestResult.channel) }} - {{ lastTestResult.success ? '成功' : '失败' }}
                <template v-if="lastTestResult.reason"> - {{ lastTestResult.reason }}</template>
              </n-tag>
            </n-form-item>
          </n-form>
        </n-card>
      </n-tab-pane>
    </n-tabs>

    <!-- 模板编辑抽屉 -->
    <n-drawer v-model:show="templateFormShow" :width="520">
      <n-drawer-content :title="editingTemplate.id ? '编辑模板' : '新增模板'">
        <n-form
          ref="templateFormRef"
          :model="editingTemplate"
          label-placement="left"
          label-width="120"
        >
          <n-form-item label="业务类型" path="bizType">
            <n-input v-model:value="editingTemplate.bizType" placeholder="approval / customer / ..." />
          </n-form-item>
          <n-form-item label="事件 Code" path="eventCode">
            <n-input v-model:value="editingTemplate.eventCode" placeholder="created / passed / rejected" />
          </n-form-item>
          <n-form-item label="模板 ID" path="templateId">
            <n-input v-model:value="editingTemplate.templateId" placeholder="微信公众平台申请的模板 ID" />
          </n-form-item>
          <n-form-item label="字段映射 JSON">
            <n-input
              v-model:value="editingTemplate.fieldMappingJson"
              type="textarea"
              :rows="4"
              placeholder='{"applicantName":"thing1","status":"phrase2"}'
            />
          </n-form-item>
          <n-form-item label="默认页面">
            <n-input v-model:value="editingTemplate.defaultPage" placeholder="/pages/approval/detail" />
          </n-form-item>
          <n-form-item label="启用">
            <n-switch v-model:value="editingTemplate.enabledBool" />
          </n-form-item>
          <n-form-item label="备注">
            <n-input v-model:value="editingTemplate.remark" type="textarea" :rows="2" />
          </n-form-item>
        </n-form>
        <template #footer>
          <n-button @click="templateFormShow = false">取消</n-button>
          <n-button type="primary" :loading="templateSaving" @click="saveTemplate">保存</n-button>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/**
 * V4.0 闭环⑤ D5 - 推送通道配置页（Web 端）
 *
 * 包含：
 *   1) 订阅消息模板的增/改/删/查
 *   2) 三通道下发日志查询
 *   3) 管理员触发一次推送，验证三通道决策链
 */
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton, NCard, NDataTable, NDrawer, NDrawerContent,
  NForm, NFormItem, NIcon, NInput, NInputNumber, NSelect,
  NSwitch, NTabPane, NTabs, NTag, useMessage
} from 'naive-ui'
import { AddOutline } from '@vicons/ionicons5'
import {
  type ChannelPushTestRequest,
  type ChannelPushTestResponse,
  type PushChannelLogDTO,
  type PushChannelLogQuery,
  type SubscribeMsgTemplateDTO,
  deleteSubscribeTemplate,
  listPushChannelLogs,
  listSubscribeTemplates,
  saveSubscribeTemplate,
  testChannelPush
} from '@/api/pushChannel'

const message = useMessage()
const activeTab = ref<'templates' | 'logs' | 'test'>('templates')

// -----------------------------------------------------------------------------
// 模板列表
// -----------------------------------------------------------------------------
interface TemplateRow extends SubscribeMsgTemplateDTO {
  enabledBool?: boolean
}

const templates = ref<TemplateRow[]>([])
const templatesLoading = ref(false)
const templateFormShow = ref(false)
const templateSaving = ref(false)
const editingTemplate = reactive<TemplateRow>({
  bizType: '',
  eventCode: '',
  templateId: '',
  fieldMappingJson: '',
  defaultPage: '',
  enabled: 1,
  enabledBool: true,
  remark: ''
})

const templateColumns = [
  { title: '业务类型', key: 'bizType' },
  { title: '事件', key: 'eventCode' },
  { title: '模板 ID', key: 'templateId' },
  { title: '默认页面', key: 'defaultPage' },
  {
    title: '状态',
    key: 'enabled',
    render(row: TemplateRow) {
      return h(NTag, { type: row.enabled ? 'success' : 'default' },
        { default: () => (row.enabled ? '启用' : '禁用') })
    }
  },
  {
    title: '操作',
    key: 'actions',
    render(row: TemplateRow) {
      return [
        h(NButton, { size: 'tiny', type: 'primary', onClick: () => openTemplateForm(row) }, { default: () => '编辑' }),
        h(NButton, { size: 'tiny', type: 'error', style: 'margin-left:8px', onClick: () => removeTemplate(row) }, { default: () => '删除' })
      ]
    }
  }
]

async function loadTemplates() {
  templatesLoading.value = true
  try {
    const list = await listSubscribeTemplates()
    templates.value = (list || []).map((row) => ({ ...row, enabledBool: row.enabled === 1 }))
  } catch (err) {
    message.error('加载模板失败')
  } finally {
    templatesLoading.value = false
  }
}

function openTemplateForm(row?: TemplateRow) {
  if (row) {
    Object.assign(editingTemplate, row)
    editingTemplate.enabledBool = row.enabled === 1
  } else {
    Object.assign(editingTemplate, {
      id: undefined,
      bizType: '',
      eventCode: '',
      templateId: '',
      fieldMappingJson: '',
      defaultPage: '',
      enabled: 1,
      enabledBool: true,
      remark: ''
    })
  }
  templateFormShow.value = true
}

async function saveTemplate() {
  templateSaving.value = true
  try {
    editingTemplate.enabled = editingTemplate.enabledBool ? 1 : 0
    await saveSubscribeTemplate(editingTemplate)
    message.success('保存成功')
    templateFormShow.value = false
    await loadTemplates()
  } catch (err) {
    message.error('保存失败')
  } finally {
    templateSaving.value = false
  }
}

async function removeTemplate(row: TemplateRow) {
  if (!row.id) return
  try {
    await deleteSubscribeTemplate(row.id)
    message.success('删除成功')
    await loadTemplates()
  } catch (err) {
    message.error('删除失败')
  }
}

// -----------------------------------------------------------------------------
// 日志查询
// -----------------------------------------------------------------------------
const logs = ref<PushChannelLogDTO[]>([])
const logsLoading = ref(false)
const logQuery = reactive<PushChannelLogQuery>({
  pageNum: 1, pageSize: 20
})
const logPagination = reactive({
  page: 1, pageSize: 20, itemCount: 0,
  onChange: (page: number) => {
    logPagination.page = page
    logQuery.pageNum = page
    loadLogs()
  }
})

const channelOptions = [
  { label: 'APP 推送', value: 'appPush' },
  { label: '小程序订阅消息', value: 'mpSubscribe' },
  { label: 'Web 站内信', value: 'webInbox' },
  { label: '无可用通道', value: 'none' }
]

const successOptions = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 }
]

const logColumns = [
  { title: '用户 ID', key: 'userId' },
  {
    title: '通道', key: 'channel',
    render(row: PushChannelLogDTO) {
      return h(NTag, { type: 'info' }, { default: () => formatChannel(row.channel) })
    }
  },
  { title: '业务', key: 'bizType' },
  { title: '业务 ID', key: 'bizId' },
  { title: '标题', key: 'title' },
  {
    title: '结果', key: 'success',
    render(row: PushChannelLogDTO) {
      return h(NTag, { type: row.success ? 'success' : 'error' },
        { default: () => (row.success ? '成功' : '失败') })
    }
  },
  { title: '原因', key: 'reason' },
  { title: '时间', key: 'createTime' }
]

async function loadLogs() {
  logsLoading.value = true
  try {
    const res = await listPushChannelLogs({ ...logQuery, pageNum: logPagination.page })
    logs.value = res.records
    logPagination.itemCount = res.total
  } catch (err) {
    message.error('加载日志失败')
  } finally {
    logsLoading.value = false
  }
}

function resetLogQuery() {
  logQuery.channel = undefined
  logQuery.bizType = undefined
  logQuery.success = undefined
  logPagination.page = 1
  loadLogs()
}

function formatChannel(code: string) {
  switch (code) {
    case 'appPush': return 'APP 推送'
    case 'mpSubscribe': return '小程序订阅消息'
    case 'webInbox': return 'Web 站内信'
    case 'none': return '无可用通道'
    default: return code
  }
}

// -----------------------------------------------------------------------------
// 通道测试
// -----------------------------------------------------------------------------
const testForm = reactive<ChannelPushTestRequest>({
  userId: 1,
  bizType: 'approval',
  bizId: undefined,
  title: '测试通知',
  content: '这是一条来自 push-channel 测试页面的消息',
  subscribeTemplateId: ''
})
const testing = ref(false)
const lastTestResult = ref<ChannelPushTestResponse | null>(null)

async function runTest() {
  testing.value = true
  try {
    lastTestResult.value = await testChannelPush(testForm)
    message.info(`通道：${formatChannel(lastTestResult.value.channel)}`)
  } catch (err) {
    message.error('测试失败')
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  loadTemplates()
  loadLogs()
})
</script>

<style lang="scss" scoped>
.push-channel-page {
  padding: 16px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 {
      margin: 0 0 4px 0;
      font-size: 20px;
    }

    .desc {
      margin: 0;
      color: #888;
      font-size: 13px;
    }
  }

  .filter-form {
    margin-bottom: 16px;
  }
}
</style>
