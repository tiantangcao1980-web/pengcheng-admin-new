<template>
  <div class="meeting-page">
    <n-grid :cols="24" :x-gap="16" :y-gap="16">
      <!-- 左侧：日历视图 -->
      <n-gi :span="16">
        <n-card title="会议日历">
          <n-calendar v-model:value="selectedDate" @update:value="loadDayMeetings">
            <template #header>
              <n-space>
                <n-button size="small" @click="jumpToday">今天</n-button>
                <n-button size="small" @click="jumpPrevMonth">上月</n-button>
                <n-button size="small" @click="jumpNextMonth">下月</n-button>
              </n-space>
            </template>
            <template #default="{ year, month, date }">
              <div class="calendar-cell">
                <div v-for="event in getDayEvents(year, month, date)" :key="event.id" class="calendar-event" :class="event.type">
                  {{ event.title }}
                </div>
              </div>
            </template>
          </n-calendar>
        </n-card>

        <n-card title="当日会议" style="margin-top: 16px">
          <template #header-extra>
            <n-button type="primary" size="small" @click="openMeetingForm">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              预约会议
            </n-button>
          </template>

          <n-timeline v-if="dayMeetings.length > 0">
            <n-timeline-item
              v-for="meeting in dayMeetings"
              :key="meeting.id"
              :type="meeting.status === 2 ? 'success' : meeting.status === 3 ? 'error' : 'default'"
              :title="meeting.title"
              :content="formatMeetingTime(meeting)"
            >
              <template #footer>
                <n-space>
                  <n-tag size="small" :type="getTypeTag(meeting.type)">{{ getTypeText(meeting.type) }}</n-tag>
                  <span><n-icon><LocationOutline /></n-icon> {{ meeting.location }}</span>
                  <n-button size="tiny" @click="openMeetingDetail(meeting)">详情</n-button>
                  <n-button v-if="canEdit(meeting)" size="tiny" @click="openMeetingForm(meeting)">编辑</n-button>
                  <n-button v-if="canEdit(meeting)" size="tiny" type="error" @click="cancelMeeting(meeting)">取消</n-button>
                </n-space>
              </template>
            </n-timeline-item>
          </n-timeline>
          <n-empty v-else description="当日无会议" />
        </n-card>
      </n-gi>

      <!-- 右侧：会议统计与待办 -->
      <n-gi :span="8">
        <n-card title="会议统计">
          <n-space vertical>
            <n-statistic label="本周会议" :value="weekStats.total" />
            <n-statistic label="待参加" :value="weekStats.pending" />
            <n-statistic label="已参加" :value="weekStats.attended" />
            <n-statistic label="已取消" :value="weekStats.cancelled" />
          </n-space>
        </n-card>

        <n-card title="我的待办" style="margin-top: 16px">
          <n-list>
            <n-list-item v-for="task in meetingTasks" :key="task.id">
              <template #prefix>
                <n-checkbox v-model:checked="task.completed" @update:checked="completeTask(task)" />
              </template>
              <n-thing :title="task.title" :description="task.meetingTitle" />
            </n-list-item>
          </n-list>
        </n-card>

        <n-card title="会议提醒设置" style="margin-top: 16px">
          <n-form label-placement="left" label-width="100">
            <n-form-item label="默认提醒">
              <n-select
                v-model:value="reminderConfig.default"
                :options="reminderOptions"
                style="width: 100%"
              />
            </n-form-item>
            <n-form-item label="站内信提醒">
              <n-switch v-model:value="reminderConfig站内信" />
            </n-form-item>
            <n-form-item label="邮件提醒">
              <n-switch v-model:value="reminderConfig.email" />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" :loading="savingReminder" @click="saveReminderConfig">保存设置</n-button>
            </n-form-item>
          </n-form>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 会议详情弹窗 -->
    <n-modal v-model:show="showMeetingDetail" preset="card" title="会议详情" style="width: 600px">
      <n-descriptions v-if="currentMeeting" :column="2" bordered>
        <n-descriptions-item label="会议主题">{{ currentMeeting.title }}</n-descriptions-item>
        <n-descriptions-item label="会议类型">{{ getTypeText(currentMeeting.type) }}</n-descriptions-item>
        <n-descriptions-item label="会议时间" :span="2">{{ formatMeetingTime(currentMeeting) }}</n-descriptions-item>
        <n-descriptions-item label="会议地点" :span="2">
          <n-icon><LocationOutline /></n-icon> {{ currentMeeting.location }}
        </n-descriptions-item>
        <n-descriptions-item v-if="currentMeeting.meetingUrl" label="会议链接" :span="2">
          <n-a :href="currentMeeting.meetingUrl" target="_blank">{{ currentMeeting.meetingUrl }}</n-a>
        </n-descriptions-item>
        <n-descriptions-item label="组织者">{{ currentMeeting.organizerName }}</n-descriptions-item>
        <n-descriptions-item label="状态">
          <n-tag :type="getStatusType(currentMeeting.status)">{{ getStatusText(currentMeeting.status) }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="参会人员" :span="2">
          <n-space>
            <n-avatar v-for="p in currentMeeting.participants" :key="p.id" :src="p.avatar" :size="32">
              {{ p.name }}
            </n-avatar>
          </n-space>
        </n-descriptions-item>
      </n-descriptions>

      <n-tabs v-if="currentMeeting" type="line" style="margin-top: 16px">
        <n-tab-pane name="minutes" tab="会议纪要">
          <n-input
            v-model:value="meetingMinutes.content"
            type="textarea"
            placeholder="请输入会议纪要..."
            :rows="6"
            :disabled="!canEditMinutes"
          />
          <n-space style="margin-top: 8px" v-if="canEditMinutes">
            <n-button size="small" @click="saveMinutes">保存纪要</n-button>
          </n-space>
        </n-tab-pane>
        <n-tab-pane name="files" tab="会议文件">
          <n-upload :action="uploadUrl" :headers="uploadHeaders" multiple @finish="handleFileUpload">
            <n-button size="small">上传文件</n-button>
          </n-upload>
          <n-list style="margin-top: 12px">
            <n-list-item v-for="file in meetingFiles" :key="file.id">
              <n-space justify="space-between">
                <n-space>
                  <n-icon :component="getFileIcon(file.type)" />
                  <span>{{ file.name }}</span>
                </n-space>
                <n-button size="tiny" @click="downloadFile(file)">下载</n-button>
              </n-space>
            </n-list-item>
          </n-list>
        </n-tab-pane>
      </n-tabs>
    </n-modal>

    <!-- 预约/编辑会议弹窗 -->
    <n-modal v-model:show="showMeetingForm" preset="card" :title="formTitle" style="width: 600px">
      <n-form :model="meetingForm" label-placement="left" label-width="100">
        <n-form-item label="会议主题" required>
          <n-input v-model:value="meetingForm.title" placeholder="请输入会议主题" />
        </n-form-item>
        <n-form-item label="会议类型">
          <n-select v-model:value="meetingForm.type" :options="typeOptions" />
        </n-form-item>
        <n-form-item label="会议时间" required>
          <n-date-picker v-model:value="meetingForm.timeRange" type="datetimerange" style="width: 100%" />
        </n-form-item>
        <n-form-item label="会议地点">
          <n-input v-model:value="meetingForm.location" placeholder="会议室名称或地址" />
        </n-form-item>
        <n-form-item label="视频链接">
          <n-input v-model:value="meetingForm.meetingUrl" placeholder="视频会议链接" />
        </n-form-item>
        <n-form-item label="提前提醒">
          <n-select v-model:value="meetingForm.reminderMinutes" :options="reminderOptions" />
        </n-form-item>
        <n-form-item label="参会人员">
          <n-select
            v-model:value="meetingForm.participantIds"
            :options="userOptions"
            label-field="nickname"
            value-field="id"
            multiple
            filterable
            style="width: 100%"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showMeetingForm = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submitMeeting">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { NIcon, NTag, NButton, NAvatar, NA, NUpload, useMessage } from 'naive-ui'
import { LocationOutline, DocumentOutline, ImageOutline, VideocamOutline } from '@vicons/ionicons5'
import { request } from '@/utils/request'
import { userApi } from '@/api/system'

const message = useMessage()

const selectedDate = ref(Date.now())
const calendarEvents = ref<any[]>([])
const dayMeetings = ref<any[]>([])

const weekStats = ref({ total: 5, pending: 2, attended: 2, cancelled: 1 })
const meetingTasks = ref<any[]>([
  { id: 1, title: '准备会议材料', meetingTitle: '周例会', completed: false },
  { id: 2, title: '发送会议邀请', meetingTitle: '项目评审会', completed: true }
])

const reminderConfig = ref({ default: 15, '站内信': true, email: false })
const savingReminder = ref(false)

const showMeetingDetail = ref(false)
const showMeetingForm = ref(false)
const currentMeeting = ref<any>(null)
const meetingForm = reactive({
  id: null as number | null,
  title: '',
  type: 1,
  timeRange: null as [number, number] | null,
  location: '',
  meetingUrl: '',
  reminderMinutes: 15,
  participantIds: [] as number[]
})
const meetingMinutes = reactive({ content: '', conclusions: '', actionItems: '' })
const meetingFiles = ref<any[]>([])
const canEditMinutes = ref(true)

const userOptions = ref<any[]>([])
const submitting = ref(false)

const typeOptions = [
  { label: '普通会议', value: 1 },
  { label: '视频会议', value: 2 },
  { label: '电话会议', value: 3 }
]

const reminderOptions = [
  { label: '不提醒', value: 0 },
  { label: '5 分钟前', value: 5 },
  { label: '10 分钟前', value: 10 },
  { label: '15 分钟前', value: 15 },
  { label: '30 分钟前', value: 30 },
  { label: '1 小时前', value: 60 },
  { label: '1 天前', value: 1440 }
]

const uploadUrl = '/api/sys/file/upload'
const uploadHeaders = { Authorization: 'Bearer ' + localStorage.getItem('token') }

function getDayEvents(year: number, month: number, date: number) {
  const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(date).padStart(2, '0')}`
  return calendarEvents.value.filter(e => e.date === dateStr)
}

function loadDayMeetings() {
  const date = new Date(selectedDate.value)
  const dateStr = date.toISOString().slice(0, 10)
  // TODO: 调用 API 获取当日会议
  dayMeetings.value = []
}

function jumpToday() { selectedDate.value = Date.now() }
function jumpPrevMonth() { selectedDate.value = new Date(selectedDate.value).setMonth(new Date(selectedDate.value).getMonth() - 1) }
function jumpNextMonth() { selectedDate.value = new Date(selectedDate.value).setMonth(new Date(selectedDate.value).getMonth() + 1) }

function formatMeetingTime(meeting: any) {
  const start = new Date(meeting.startTime)
  const end = new Date(meeting.endTime)
  return `${start.toLocaleDateString()} ${start.toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'})} - ${end.toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'})}`
}

function getTypeText(type: number) {
  const map: Record<number, string> = { 1: '普通会议', 2: '视频会议', 3: '电话会议' }
  return map[type] || '未知'
}

function getTypeTag(type: number): 'default' | 'info' | 'success' | 'warning' | 'error' {
  const map: Record<number, any> = { 1: 'default', 2: 'info', 3: 'success' }
  return map[type] || 'default'
}

function getStatusText(status: number) {
  const map: Record<number, string> = { 1: '未开始', 2: '进行中', 3: '已取消', 4: '已结束' }
  return map[status] || '未知'
}

function getStatusType(status: number): 'default' | 'info' | 'success' | 'warning' | 'error' {
  const map: Record<number, any> = { 1: 'default', 2: 'info', 3: 'error', 4: 'success' }
  return map[status] || 'default'
}

function canEdit(meeting: any) {
  return meeting.organizerId === 1 // TODO: 当前用户 ID
}

function openMeetingDetail(meeting: any) {
  currentMeeting.value = meeting
  showMeetingDetail.value = true
}

function openMeetingForm(meeting?: any) {
  if (meeting) {
    meetingForm.id = meeting.id
    meetingForm.title = meeting.title
    meetingForm.type = meeting.type
    meetingForm.timeRange = [new Date(meeting.startTime).getTime(), new Date(meeting.endTime).getTime()]
    meetingForm.location = meeting.location
    meetingForm.meetingUrl = meeting.meetingUrl || ''
    meetingForm.reminderMinutes = meeting.reminderMinutes || 15
  } else {
    meetingForm.id = null
    meetingForm.title = ''
    meetingForm.type = 1
    meetingForm.timeRange = null
    meetingForm.location = ''
    meetingForm.meetingUrl = ''
    meetingForm.reminderMinutes = 15
  }
  showMeetingForm.value = true
}

function submitMeeting() {
  if (!meetingForm.title || !meetingForm.timeRange) {
    message.warning('请填写必填项')
    return
  }
  submitting.value = true
  // TODO: 调用 API 保存会议
  setTimeout(() => {
    message.success('会议已保存')
    showMeetingForm.value = false
    submitting.value = false
  }, 500)
}

function cancelMeeting(meeting: any) {
  if (!confirm('确定取消该会议吗？')) return
  // TODO: 调用 API 取消会议
  message.success('会议已取消')
}

function saveMinutes() {
  // TODO: 调用 API 保存会议纪要
  message.success('纪要已保存')
}

function handleFileUpload({ event }: { event: Event }) {
  // TODO: 处理文件上传完成
  message.success('文件上传成功')
}

function downloadFile(file: any) {
  window.open(file.url, '_blank')
}

function getFileIcon(type: string) {
  if (type.startsWith('image/')) return ImageOutline
  if (type.startsWith('video/')) return VideocamOutline
  return DocumentOutline
}

function completeTask(task: any) {
  // TODO: 调用 API 更新任务状态
}

async function loadUserOptions() {
  try {
    const res: any = await userApi.page({ page: 1, pageSize: 100 })
    userOptions.value = res?.list || []
  } catch {}
}

async function saveReminderConfig() {
  savingReminder.value = true
  // TODO: 调用 API 保存提醒配置
  setTimeout(() => {
    message.success('设置已保存')
    savingReminder.value = false
  }, 500)
}

onMounted(() => {
  loadDayMeetings()
  loadUserOptions()
})
</script>

<style scoped>
.meeting-page { padding: 20px; }
.calendar-cell { min-height: 60px; }
.calendar-event {
  font-size: 12px;
  padding: 2px 6px;
  margin: 2px 0;
  border-radius: 4px;
  background: rgba(24, 160, 88, 0.2);
  color: #18a058;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.calendar-event.video { background: rgba(0, 132, 255, 0.2); color: #0084ff; }
.calendar-event.phone { background: rgba(82, 196, 26, 0.2); color: #52c41a; }
</style>
