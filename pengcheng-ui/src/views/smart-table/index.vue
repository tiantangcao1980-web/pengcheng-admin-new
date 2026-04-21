<template>
  <div class="page-container">
    <div class="smart-table-layout">
      <!-- 顶部工具栏 -->
      <div class="table-header">
        <div class="header-left">
          <n-icon size="24" color="#18a058"><AppsOutline /></n-icon>
          <span class="table-title">项目任务进度表</span>
          <div class="view-switcher">
            <n-radio-group v-model:value="currentView" size="small">
              <n-radio-button value="grid">
                <template #icon><n-icon><GridOutline /></n-icon></template>
                表格
              </n-radio-button>
              <n-radio-button value="kanban">
                <template #icon><n-icon><ListOutline /></n-icon></template>
                看板
              </n-radio-button>
              <n-radio-button value="gantt" disabled>
                <template #icon><n-icon><BarChartOutline /></n-icon></template>
                甘特图
              </n-radio-button>
              <n-radio-button value="gallery" disabled>
                <template #icon><n-icon><ImagesOutline /></n-icon></template>
                画廊
              </n-radio-button>
            </n-radio-group>
          </div>
        </div>
        <div class="header-right">
          <n-input v-model:value="searchKeyword" placeholder="搜索任务..." size="small" clearable>
            <template #prefix>
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-input>
          <n-button type="primary" size="small" @click="handleAddRow">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            新建记录
          </n-button>
          <n-popover trigger="click" placement="bottom-end">
            <template #trigger>
              <n-button size="small">
                <template #icon><n-icon><SettingsOutline /></n-icon></template>
                字段
              </n-button>
            </template>
            <div class="column-settings">
              <n-checkbox-group v-model:value="visibleColumns">
                <div v-for="col in allColumns" :key="col.key" class="column-item">
                  <n-checkbox :value="col.key">{{ col.title }}</n-checkbox>
                </div>
              </n-checkbox-group>
            </div>
          </n-popover>
        </div>
      </div>

      <!-- 表格视图 -->
      <div v-if="currentView === 'grid'" class="table-content">
        <n-data-table
          :columns="tableColumns"
          :data="filteredData"
          :pagination="pagination"
          :row-key="row => row.id"
          :single-line="false"
          size="small"
        />
      </div>

      <!-- 看板视图 -->
      <div v-else class="kanban-content">
        <div class="kanban-board">
          <div v-for="status in statusOptions" :key="status.value" class="kanban-column">
            <div class="kanban-header">
              <n-tag :type="status.type" size="small" round>{{ status.label }}</n-tag>
              <span class="count">{{ getTasksByStatus(status.value).length }}</span>
            </div>
            <div class="kanban-tasks">
              <div 
                v-for="task in getTasksByStatus(status.value)" 
                :key="task.id" 
                class="kanban-card"
                draggable="true"
                @dragstart="handleDragStart(task)"
                @dragover.prevent
                @drop="handleDrop(status.value)"
              >
                <div class="card-title">{{ task.title }}</div>
                <div class="card-meta">
                  <div class="card-tags">
                    <n-tag v-for="tag in task.tags" :key="tag" size="tiny" type="info">{{ tag }}</n-tag>
                  </div>
                  <n-avatar-group :options="getAssignees(task.assignee)" :size="20" :max="3" />
                </div>
                <div class="card-footer">
                  <span class="date" :class="{ overdue: isOverdue(task.dueDate) }">
                    <n-icon><CalendarOutline /></n-icon>
                    {{ formatDate(task.dueDate) }}
                  </span>
                  <n-progress 
                    type="line" 
                    :percentage="task.progress" 
                    :color="getProgressColor(task.progress)"
                    :height="4"
                    :show-indicator="false" 
                    style="width: 60px"
                  />
                </div>
              </div>
              <div 
                class="kanban-placeholder" 
                @dragover.prevent
                @drop="handleDrop(status.value)"
              >
                + 添加至此处
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <n-modal v-model:show="showEditModal" preset="card" title="编辑任务" style="width: 600px">
      <n-form label-placement="left" label-width="80">
        <n-form-item label="任务名称">
          <n-input v-model:value="editingTask.title" placeholder="请输入任务名称" />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="editingTask.status" :options="statusOptions" />
        </n-form-item>
        <n-form-item label="优先级">
          <n-select v-model:value="editingTask.priority" :options="priorityOptions" />
        </n-form-item>
        <n-form-item label="负责人">
          <n-select multiple v-model:value="editingTask.assignee" :options="userOptions" />
        </n-form-item>
        <n-form-item label="截止日期">
          <n-date-picker v-model:value="editingTask.dueDate" type="date" clearable />
        </n-form-item>
        <n-form-item label="进度">
          <n-slider v-model:value="editingTask.progress" :step="5" />
        </n-form-item>
        <n-form-item label="标签">
          <n-dynamic-tags v-model:value="editingTask.tags" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showEditModal = false">取消</n-button>
          <n-button type="primary" @click="handleSaveTask">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h, reactive, onMounted } from 'vue'
import { userApi } from '@/api/system'
import { NTag, NProgress, NAvatarGroup, NButton, NSpace, NDropdown } from 'naive-ui'
import { 
  AppsOutline, GridOutline, ListOutline, SearchOutline, AddOutline, 
  SettingsOutline, CalendarOutline, EllipsisHorizontal,
  BarChartOutline, ImagesOutline
} from '@vicons/ionicons5'

// 类型定义
interface Task {
  id: number
  title: string
  status: string
  priority: string
  assignee: number[]
  dueDate: number
  progress: number
  tags: string[]
}

// 状态选项
const statusOptions = [
  { label: '未开始', value: 'todo', type: 'default' },
  { label: '进行中', value: 'in_progress', type: 'info' },
  { label: '已完成', value: 'done', type: 'success' },
  { label: '搁置', value: 'blocked', type: 'error' }
]

// 优先级选项
const priorityOptions = [
  { label: '高', value: 'high', type: 'error' },
  { label: '中', value: 'medium', type: 'warning' },
  { label: '低', value: 'low', type: 'info' }
]

// 用户选项（从 /sys/user/page 加载）
const userOptions = ref<Array<{ label: string; value: number; src: string }>>([])

async function loadUserOptions() {
  try {
    const res = await userApi.page({ page: 1, pageSize: 200 })
    const list = res?.list ?? []
    userOptions.value = list.map((u: any) => ({
      label: u.nickname || u.username || String(u.id),
      value: u.id,
      src: u.avatar || ''
    }))
  } catch {
    userOptions.value = []
  }
}

// 初始数据
const data = ref<Task[]>([
  {
    id: 1,
    title: '设计新版首页 UI',
    status: 'in_progress',
    priority: 'high',
    assignee: [1, 2],
    dueDate: Date.now() + 86400000 * 2,
    progress: 60,
    tags: ['UI/UX', '设计']
  },
  {
    id: 2,
    title: '后端 API 接口开发',
    status: 'todo',
    priority: 'high',
    assignee: [3],
    dueDate: Date.now() + 86400000 * 5,
    progress: 0,
    tags: ['后端', 'API']
  },
  {
    id: 3,
    title: '编写用户手册',
    status: 'done',
    priority: 'low',
    assignee: [4],
    dueDate: Date.now() - 86400000,
    progress: 100,
    tags: ['文档']
  },
  {
    id: 4,
    title: '修复登录 Bug',
    status: 'blocked',
    priority: 'medium',
    assignee: [2],
    dueDate: Date.now() + 86400000,
    progress: 30,
    tags: ['Bugfix']
  }
])

// 视图控制
const currentView = ref('grid')
const searchKeyword = ref('')
const showEditModal = ref(false)
const editingTask = ref<any>({})
const draggedTask = ref<Task | null>(null)

// 列定义
const allColumns = [
  { title: '任务名称', key: 'title', width: 200 },
  { title: '状态', key: 'status', width: 100 },
  { title: '优先级', key: 'priority', width: 80 },
  { title: '负责人', key: 'assignee', width: 120 },
  { title: '截止日期', key: 'dueDate', width: 120 },
  { title: '进度', key: 'progress', width: 150 },
  { title: '标签', key: 'tags', width: 150 },
  { title: '操作', key: 'actions', width: 80 }
]

const visibleColumns = ref(allColumns.map(c => c.key))

const tableColumns = computed(() => {
  return allColumns
    .filter(col => visibleColumns.value.includes(col.key))
    .map(col => {
      const baseCol: any = { ...col }
      
      // 自定义渲染
      if (col.key === 'status') {
        baseCol.render = (row: Task) => {
          const status = statusOptions.find(s => s.value === row.status)
          return h(NTag, { type: status?.type as any, bordered: false, round: true, size: 'small' }, { default: () => status?.label })
        }
      } else if (col.key === 'priority') {
        baseCol.render = (row: Task) => {
          const priority = priorityOptions.find(p => p.value === row.priority)
          return h(NTag, { type: priority?.type as any, size: 'small' }, { default: () => priority?.label })
        }
      } else if (col.key === 'assignee') {
        baseCol.render = (row: Task) => {
          const users = row.assignee.map(id => userOptions.value.find(u => u.value === id)).filter(Boolean)
          return h(NAvatarGroup, { options: users?.map(u => ({ name: u!.label, src: u!.src })) as any || [], size: 24, max: 3 })
        }
      } else if (col.key === 'dueDate') {
        baseCol.render = (row: Task) => {
          const date = new Date(row.dueDate)
          const isOver = isOverdue(row.dueDate) && row.status !== 'done'
          return h('span', { style: { color: isOver ? '#d03050' : 'inherit' } }, formatDate(row.dueDate))
        }
      } else if (col.key === 'progress') {
        baseCol.render = (row: Task) => {
          return h(NProgress, { 
            type: 'line', 
            percentage: row.progress,
            color: getProgressColor(row.progress),
            height: 6
          })
        }
      } else if (col.key === 'tags') {
        baseCol.render = (row: Task) => {
          return h(NSpace, { size: 4 }, { 
            default: () => row.tags.map(tag => h(NTag, { size: 'small', type: 'default' }, { default: () => tag }))
          })
        }
      } else if (col.key === 'actions') {
        baseCol.render = (row: Task) => {
          return h(NButton, { size: 'tiny', onClick: () => handleEdit(row) }, { default: () => '编辑' })
        }
      }
      
      return baseCol
    })
})

const filteredData = computed(() => {
  if (!searchKeyword.value) return data.value
  const keyword = searchKeyword.value.toLowerCase()
  return data.value.filter(item => 
    item.title.toLowerCase().includes(keyword) || 
    item.tags.some(tag => tag.toLowerCase().includes(keyword))
  )
})

const pagination = reactive({
  pageSize: 10
})

// 方法
function getTasksByStatus(status: string) {
  return filteredData.value.filter(task => task.status === status)
}

function getAssignees(ids: number[]) {
  return ids.map(id => {
    const user = userOptions.value.find(u => u.value === id)
    return user ? { name: user.label, src: user.src } : { name: '未知' }
  })
}

function formatDate(ts: number) {
  const d = new Date(ts)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function isOverdue(ts: number) {
  return ts < Date.now()
}

function getProgressColor(progress: number) {
  if (progress < 30) return '#d03050'
  if (progress < 70) return '#f0a020'
  return '#18a058'
}

function handleAddRow() {
  editingTask.value = {
    title: '',
    status: 'todo',
    priority: 'medium',
    assignee: [],
    dueDate: Date.now(),
    progress: 0,
    tags: []
  }
  showEditModal.value = true
}

function handleEdit(row: Task) {
  editingTask.value = JSON.parse(JSON.stringify(row))
  showEditModal.value = true
}

function handleSaveTask() {
  if (editingTask.value.id) {
    const index = data.value.findIndex(t => t.id === editingTask.value.id)
    if (index > -1) {
      data.value[index] = { ...editingTask.value }
    }
  } else {
    data.value.push({
      ...editingTask.value,
      id: Date.now()
    })
  }
  showEditModal.value = false
}

// 拖拽处理
function handleDragStart(task: Task) {
  draggedTask.value = task
}

function handleDrop(status: string) {
  if (draggedTask.value) {
    draggedTask.value.status = status
    draggedTask.value = null
  }
}

onMounted(loadUserOptions)
</script>

<style scoped>
.page-container {
  height: calc(100vh - 92px);
  background: #fff;
  display: flex;
  flex-direction: column;
}

.smart-table-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.table-header {
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.view-switcher {
  margin-left: 16px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.column-settings {
  padding: 12px;
  min-width: 150px;
}

.column-item {
  margin-bottom: 8px;
}

/* 表格视图 */
.table-content {
  flex: 1;
  padding: 16px;
  overflow: hidden;
}

/* 看板视图 */
.kanban-content {
  flex: 1;
  padding: 16px;
  overflow-x: auto;
  background: #f5f7fa;
}

.kanban-board {
  display: flex;
  gap: 16px;
  height: 100%;
}

.kanban-column {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  border-radius: 8px;
  padding: 12px;
}

.kanban-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 0 4px;
}

.count {
  font-size: 12px;
  color: #999;
}

.kanban-tasks {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kanban-card {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
  cursor: grab;
  transition: all 0.2s;
}

.kanban-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

.kanban-card:active {
  cursor: grabbing;
}

.card-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.date {
  display: flex;
  align-items: center;
  gap: 4px;
}

.date.overdue {
  color: #d03050;
}

.kanban-placeholder {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #ccc;
  border-radius: 6px;
  color: #999;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.kanban-placeholder:hover {
  background: #e8f5e9;
  border-color: #18a058;
  color: #18a058;
}
</style>