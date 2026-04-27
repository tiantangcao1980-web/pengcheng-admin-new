<template>
  <div class="kanban-page">
    <!-- 工具栏 -->
    <div class="kanban-toolbar">
      <n-select
        v-model:value="projectId"
        :options="projectOptions"
        placeholder="选择项目"
        style="width: 220px"
        @update:value="onProjectChange"
      />
      <n-select
        v-model:value="filterAssignee"
        :options="memberOptions"
        clearable
        placeholder="负责人"
        style="width: 140px"
        @update:value="applyFilter"
      />
      <n-select
        v-model:value="filterPriority"
        :options="priorityOptions"
        clearable
        placeholder="优先级"
        style="width: 120px"
        @update:value="applyFilter"
      />
      <n-input
        v-model:value="filterKeyword"
        placeholder="搜索任务"
        clearable
        style="width: 160px"
        @update:value="applyFilter"
      />
      <n-button type="primary" size="small" @click="openCreateTask(null)">+ 新建任务</n-button>
    </div>

    <div v-if="loading" class="kanban-loading">
      <n-spin />
    </div>

    <div v-else class="kanban-body">
      <div
        v-for="col in columns"
        :key="col.id"
        class="kanban-col"
        :class="{ 'drag-over': dragOverColId === col.id }"
        @dragover.prevent="dragOverColId = col.id"
        @dragleave="onColDragLeave(col.id)"
        @drop.prevent="onDrop(col)"
      >
        <!-- 列头 -->
        <div class="col-header">
          <span class="col-title">{{ col.name }}</span>
          <span class="col-count">{{ filteredCards(col).length }}</span>
          <n-button text size="tiny" @click="openCreateTask(col.statusValue)">+</n-button>
        </div>

        <!-- 卡片列表 -->
        <div class="col-cards">
          <div
            v-for="card in filteredCards(col)"
            :key="card.id"
            class="kanban-card"
            :class="`priority-${card.priority ?? 0}`"
            draggable="true"
            @dragstart="onDragStart($event, card, col)"
            @dragend="onDragEnd"
            @click="openDrawer(card.id)"
          >
            <div class="card-title">{{ card.title }}</div>
            <div class="card-meta">
              <span v-if="card.dueDate" class="card-due" :class="{ overdue: isOverdue(card) }">
                {{ card.dueDate }}
              </span>
              <div class="card-progress-bar">
                <div class="card-progress-fill" :style="{ width: (card.progress ?? 0) + '%' }" />
              </div>
              <span class="card-progress-text">{{ card.progress ?? 0 }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建任务弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="card" title="新建任务" style="width: 440px">
      <n-form :model="createForm" label-width="80px" label-placement="left">
        <n-form-item label="标题" required>
          <n-input v-model:value="createForm.title" placeholder="任务标题" />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="createForm.status" :options="columnOptions" />
        </n-form-item>
        <n-form-item label="优先级">
          <n-select v-model:value="createForm.priority" :options="priorityOptions" />
        </n-form-item>
        <n-form-item label="截止日期">
          <n-date-picker
            v-model:value="createForm.dueDateTs"
            type="date"
            value-format="yyyy-MM-dd"
            clearable
            style="width: 100%"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreateModal = false">取消</n-button>
          <n-button type="primary" :disabled="!createForm.title?.trim()" @click="submitCreate">创建</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 任务详情 drawer -->
    <TaskDrawer v-model:show="drawerVisible" :task-id="drawerTaskId" @saved="loadBoard" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { NSelect, NInput, NButton, NSpin, NModal, NForm, NFormItem, NSpace, NDatePicker } from 'naive-ui'
import { useMessage } from 'naive-ui'
import { projectApi, listColumns, updateTaskStatus, projectTaskApi } from '@/api/project'
import type { PmTask, PmStatusColumn } from '@/api/project'
import TaskDrawer from '../components/TaskDrawer.vue'

const message = useMessage()

// ---- 状态 ----
const projectId = ref<number | null>(null)
const projectOptions = ref<{ label: string; value: number }[]>([])
const columns = ref<PmStatusColumn[]>([])
const allCards = ref<PmTask[]>([])
const loading = ref(false)
const dragOverColId = ref<number | null>(null)
const drawerVisible = ref(false)
const drawerTaskId = ref<number | null>(null)
const showCreateModal = ref(false)

// 筛选
const filterAssignee = ref<number | null>(null)
const filterPriority = ref<number | null>(null)
const filterKeyword = ref('')
const memberOptions = ref<{ label: string; value: number }[]>([])

const priorityOptions = [
  { label: '低', value: 1 },
  { label: '中', value: 2 },
  { label: '高', value: 3 },
  { label: '紧急', value: 4 },
]

const columnOptions = computed(() =>
  columns.value.map(c => ({ label: c.name, value: c.statusValue }))
)

const createForm = ref({
  title: '',
  status: '',
  priority: null as number | null,
  dueDateTs: null as number | null,
})

// ---- 拖拽状态 ----
let draggedCard: { card: PmTask; fromCol: PmStatusColumn } | null = null

function onDragStart(e: DragEvent, card: PmTask, col: PmStatusColumn) {
  draggedCard = { card, fromCol: col }
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(card.id))
  }
}

function onDragEnd() {
  dragOverColId.value = null
  draggedCard = null
}

function onColDragLeave(colId: number) {
  if (dragOverColId.value === colId) dragOverColId.value = null
}

async function onDrop(targetCol: PmStatusColumn) {
  dragOverColId.value = null
  if (!draggedCard) return
  const { card, fromCol } = draggedCard
  draggedCard = null
  if (fromCol.statusValue === targetCol.statusValue) return

  // 乐观更新
  const oldStatus = card.status
  card.status = targetCol.statusValue

  try {
    await updateTaskStatus(card.id, targetCol.statusValue)
    message.success('已移动到 ' + targetCol.name)
  } catch {
    card.status = oldStatus
    message.error('移动失败')
  }
}

// ---- 筛选 ----
function filteredCards(col: PmStatusColumn): PmTask[] {
  return allCards.value.filter(c => {
    if (c.status !== col.statusValue) return false
    if (filterAssignee.value != null && c.assigneeId !== filterAssignee.value) return false
    if (filterPriority.value != null && c.priority !== filterPriority.value) return false
    if (filterKeyword.value && !c.title.includes(filterKeyword.value)) return false
    return true
  })
}

function applyFilter() { /* 响应式计算，无需额外操作 */ }

function isOverdue(card: PmTask): boolean {
  if (!card.dueDate) return false
  return card.dueDate < new Date().toISOString().slice(0, 10) && (card.progress ?? 0) < 100
}

// ---- 弹窗 ----
function openDrawer(taskId: number) {
  drawerTaskId.value = taskId
  drawerVisible.value = true
}

function openCreateTask(defaultStatus: string | null) {
  createForm.value = {
    title: '',
    status: defaultStatus ?? (columns.value[0]?.statusValue ?? ''),
    priority: null,
    dueDateTs: null,
  }
  showCreateModal.value = true
}

async function submitCreate() {
  if (!projectId.value || !createForm.value.title?.trim()) return
  const dueDate = createForm.value.dueDateTs
    ? new Date(createForm.value.dueDateTs).toISOString().slice(0, 10)
    : null
  try {
    await projectTaskApi.create(projectId.value, {
      title: createForm.value.title,
      status: createForm.value.status,
      priority: createForm.value.priority,
      dueDate,
    })
    showCreateModal.value = false
    message.success('任务已创建')
    await loadBoard()
  } catch {
    message.error('创建失败')
  }
}

// ---- 数据加载 ----
const DEFAULT_COLS: PmStatusColumn[] = [
  { id: -1, projectId: 0, name: '待办', statusValue: '待办', sortOrder: 0, isDone: 0 },
  { id: -2, projectId: 0, name: '进行中', statusValue: '进行中', sortOrder: 1, isDone: 0 },
  { id: -3, projectId: 0, name: '已完成', statusValue: '已完成', sortOrder: 2, isDone: 1 },
]

async function loadBoard() {
  if (!projectId.value) return
  loading.value = true
  try {
    const [colRes, boardRes] = await Promise.all([
      listColumns(projectId.value),
      projectTaskApi.board(projectId.value),
    ])
    const colList: PmStatusColumn[] = (colRes as any)?.data ?? colRes ?? []
    columns.value = colList.length ? colList : DEFAULT_COLS

    const boardData: Record<string, PmTask[]> = (boardRes as any)?.data ?? boardRes ?? {}
    const flat: PmTask[] = []
    Object.values(boardData).forEach(arr => flat.push(...arr))
    allCards.value = flat
  } finally {
    loading.value = false
  }
}

async function loadProjects() {
  const res: any = await projectApi.list({ page: 1, size: 200 })
  const list = res?.data?.records ?? res?.records ?? res?.data ?? res ?? []
  projectOptions.value = (Array.isArray(list) ? list : []).map((p: any) => ({ label: p.name, value: p.id }))
  if (projectOptions.value.length && !projectId.value) {
    projectId.value = projectOptions.value[0].value
    await loadBoard()
    loadMembers()
  }
}

async function loadMembers() {
  if (!projectId.value) return
  try {
    const res: any = await projectApi.members(projectId.value)
    const list = res?.data ?? res ?? []
    memberOptions.value = (Array.isArray(list) ? list : []).map((m: any) => ({
      label: m.nickname ?? m.username ?? String(m.userId),
      value: m.userId,
    }))
  } catch { /* 忽略 */ }
}

async function onProjectChange() {
  await loadBoard()
  loadMembers()
}

onMounted(loadProjects)
</script>

<style scoped>
.kanban-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--n-color-modal, #f5f5f5);
}

.kanban-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--n-border-color, #e8e8e8);
  background: var(--n-color-modal, #fff);
  flex-shrink: 0;
}

.kanban-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.kanban-body {
  display: flex;
  flex: 1;
  gap: 12px;
  padding: 16px;
  overflow-x: auto;
  align-items: flex-start;
}

.kanban-col {
  width: 280px;
  flex-shrink: 0;
  background: var(--n-color, #f0f0f0);
  border-radius: 8px;
  padding: 0 0 12px;
  transition: box-shadow 0.15s;
  min-height: 200px;
}

.kanban-col.drag-over {
  box-shadow: inset 0 0 0 2px var(--n-primary-color, #1890ff);
}

.col-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px 8px;
  border-bottom: 2px solid rgba(0, 0, 0, 0.06);
}

.col-title {
  font-weight: 600;
  font-size: 14px;
  flex: 1;
}

.col-count {
  font-size: 12px;
  color: var(--n-text-color-3, #999);
  background: var(--n-border-color, #e8e8e8);
  border-radius: 10px;
  padding: 0 6px;
}

.col-cards {
  padding: 8px 8px 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kanban-card {
  background: var(--n-color-modal, #fff);
  border-radius: 6px;
  padding: 10px 12px;
  cursor: pointer;
  border-left: 3px solid transparent;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.15s;
}

.kanban-card:hover {
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.14);
}

.kanban-card:active {
  cursor: grabbing;
}

/* 优先级左边框色 */
.kanban-card.priority-1 { border-left-color: #52c41a; }
.kanban-card.priority-2 { border-left-color: #faad14; }
.kanban-card.priority-3 { border-left-color: #ff7a45; }
.kanban-card.priority-4 { border-left-color: #f5222d; }

.card-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 6px;
  word-break: break-all;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
}

.card-due {
  color: var(--n-text-color-3, #999);
}

.card-due.overdue {
  color: var(--n-error-color, #ff4d4f);
  font-weight: 500;
}

.card-progress-bar {
  flex: 1;
  height: 4px;
  background: var(--n-border-color, #e8e8e8);
  border-radius: 2px;
  overflow: hidden;
}

.card-progress-fill {
  height: 100%;
  background: var(--n-primary-color, #1890ff);
  border-radius: 2px;
  transition: width 0.3s;
}

.card-progress-text {
  color: var(--n-text-color-3, #999);
  white-space: nowrap;
}
</style>
