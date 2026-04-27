<template>
  <div class="gantt-page">
    <!-- 工具栏 -->
    <div class="gantt-toolbar">
      <n-select
        v-model:value="projectId"
        :options="projectOptions"
        placeholder="选择项目"
        style="width: 220px"
        @update:value="loadData"
      />
      <n-radio-group v-model:value="scale" size="small" @update:value="onScaleChange">
        <n-radio-button value="day">日</n-radio-button>
        <n-radio-button value="week">周</n-radio-button>
        <n-radio-button value="month">月</n-radio-button>
      </n-radio-group>
      <n-button size="small" @click="scrollToToday">今天</n-button>
    </div>

    <div v-if="loading" class="gantt-loading">
      <n-spin />
    </div>

    <div v-else class="gantt-body" ref="ganttBodyRef">
      <!-- 左侧任务列表 -->
      <div class="gantt-left" ref="ganttLeftRef">
        <div class="gantt-left-header">
          <span>任务名称</span>
        </div>
        <div
          v-for="row in flatRows"
          :key="row.id"
          class="gantt-row-label"
          :style="{ paddingLeft: `${row.depth * 20 + 12}px` }"
          :class="{ 'is-parent': row.hasChildren }"
        >
          <span v-if="row.hasChildren" class="expand-icon" @click="toggleExpand(row.id)">
            {{ expandedIds.has(row.id) ? '▾' : '▸' }}
          </span>
          <span class="task-name" :title="row.title">{{ row.title }}</span>
        </div>
        <!-- 里程碑行 -->
        <div
          v-for="ms in milestones"
          :key="'ms-' + ms.id"
          class="gantt-row-label milestone-label"
        >
          <span class="ms-icon">◆</span>
          <span class="task-name" :title="ms.name">{{ ms.name }}</span>
        </div>
      </div>

      <!-- 右侧时间轴 -->
      <div class="gantt-right" ref="ganttRightRef" @scroll="syncScroll">
        <!-- 时间刻度头部 -->
        <div class="gantt-time-header" :style="{ width: totalWidth + 'px' }">
          <div
            v-for="col in timeColumns"
            :key="col.key"
            class="time-col-header"
            :style="{ width: col.width + 'px' }"
          >{{ col.label }}</div>
        </div>

        <!-- 今天竖线 -->
        <div
          v-if="todayLeft >= 0"
          class="today-line"
          :style="{ left: todayLeft + 'px', height: (flatRows.length + milestones.length) * ROW_HEIGHT + 'px' }"
        />

        <!-- SVG 依赖线层 -->
        <svg
          class="dep-svg"
          :width="totalWidth"
          :height="(flatRows.length + milestones.length) * ROW_HEIGHT"
        >
          <path
            v-for="dep in depPaths"
            :key="dep.key"
            :d="dep.d"
            class="dep-path"
          />
        </svg>

        <!-- 任务条 -->
        <div
          class="gantt-chart-area"
          :style="{ width: totalWidth + 'px' }"
        >
          <div
            v-for="(row, idx) in flatRows"
            :key="row.id"
            class="gantt-row"
            :style="{ height: ROW_HEIGHT + 'px' }"
          >
            <template v-if="row.startDate && row.endDate">
              <!-- 任务条主体 -->
              <div
                class="task-bar"
                :class="barClass(row)"
                :style="barStyle(row)"
                :title="`${row.title} (${row.progress}%)`"
                @mousedown="onBarMousedown($event, row, 'move')"
              >
                <div class="bar-progress" :style="{ width: row.progress + '%' }" />
                <span class="bar-label">{{ row.title }}</span>
                <!-- 左边角（改 startDate） -->
                <div class="resize-handle left" @mousedown.stop="onBarMousedown($event, row, 'left')" />
                <!-- 右边角（改 endDate） -->
                <div class="resize-handle right" @mousedown.stop="onBarMousedown($event, row, 'right')" />
              </div>
            </template>
          </div>

          <!-- 里程碑菱形 -->
          <div
            v-for="ms in milestones"
            :key="'ms-bar-' + ms.id"
            class="gantt-row"
            :style="{ height: ROW_HEIGHT + 'px' }"
          >
            <svg
              v-if="ms.dueDate"
              class="milestone-diamond"
              :style="{ left: milestoneLeft(ms) + 'px', top: '50%', transform: 'translate(-50%, -50%)' }"
              width="16" height="16"
            >
              <polygon points="8,0 16,8 8,16 0,8" :fill="ms.status === 1 ? '#52c41a' : '#faad14'" />
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- 任务详情 drawer -->
    <TaskDrawer v-model:show="drawerVisible" :task-id="drawerTaskId" @saved="loadData" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { NSelect, NRadioGroup, NRadioButton, NButton, NSpin } from 'naive-ui'
import { projectApi, listTasks, listMilestones, updateTaskTime } from '@/api/project'
import type { PmTask, PmMilestone } from '@/api/project'
import TaskDrawer from '../components/TaskDrawer.vue'

// ---- 常量 ----
const ROW_HEIGHT = 40
const DAY_PX = { day: 40, week: 20, month: 10 } as const

// ---- 状态 ----
const projectId = ref<number | null>(null)
const projectOptions = ref<{ label: string; value: number }[]>([])
const scale = ref<'day' | 'week' | 'month'>('week')
const loading = ref(false)
const tasks = ref<PmTask[]>([])
const milestones = ref<PmMilestone[]>([])
const expandedIds = ref<Set<number>>(new Set())
const drawerVisible = ref(false)
const drawerTaskId = ref<number | null>(null)
const ganttRightRef = ref<HTMLElement | null>(null)
const ganttBodyRef = ref<HTMLElement | null>(null)

// ---- 时间范围 ----
const viewStart = ref<Date>(new Date())
const viewEnd = ref<Date>(new Date())
const dayPx = computed(() => DAY_PX[scale.value])

function computeViewRange(taskList: PmTask[], msList: PmMilestone[]) {
  const dates: Date[] = []
  taskList.forEach(t => {
    if (t.startDate) dates.push(new Date(t.startDate))
    if (t.endDate) dates.push(new Date(t.endDate))
  })
  msList.forEach(m => { if (m.dueDate) dates.push(new Date(m.dueDate)) })
  const today = new Date()
  dates.push(today)
  if (!dates.length) {
    viewStart.value = new Date(today.getFullYear(), today.getMonth(), 1)
    viewEnd.value = new Date(today.getFullYear(), today.getMonth() + 3, 0)
    return
  }
  const min = new Date(Math.min(...dates.map(d => d.getTime())))
  const max = new Date(Math.max(...dates.map(d => d.getTime())))
  min.setDate(min.getDate() - 7)
  max.setDate(max.getDate() + 14)
  viewStart.value = min
  viewEnd.value = max
}

// ---- 时间列 ----
interface TimeCol { key: string; label: string; width: number }
const timeColumns = computed<TimeCol[]>(() => {
  const cols: TimeCol[] = []
  const s = new Date(viewStart.value)
  const e = viewEnd.value
  if (scale.value === 'day') {
    const cur = new Date(s)
    while (cur <= e) {
      cols.push({ key: cur.toISOString().slice(0, 10), label: `${cur.getMonth() + 1}/${cur.getDate()}`, width: dayPx.value })
      cur.setDate(cur.getDate() + 1)
    }
  } else if (scale.value === 'week') {
    const cur = new Date(s)
    // 对齐到周一
    cur.setDate(cur.getDate() - ((cur.getDay() + 6) % 7))
    while (cur <= e) {
      const label = `${cur.getMonth() + 1}/${cur.getDate()}`
      cols.push({ key: cur.toISOString().slice(0, 10), label, width: dayPx.value * 7 })
      cur.setDate(cur.getDate() + 7)
    }
  } else {
    const cur = new Date(s.getFullYear(), s.getMonth(), 1)
    while (cur <= e) {
      const daysInMonth = new Date(cur.getFullYear(), cur.getMonth() + 1, 0).getDate()
      cols.push({ key: `${cur.getFullYear()}-${cur.getMonth() + 1}`, label: `${cur.getFullYear()}/${cur.getMonth() + 1}月`, width: daysInMonth * dayPx.value })
      cur.setMonth(cur.getMonth() + 1)
    }
  }
  return cols
})

const totalWidth = computed(() => timeColumns.value.reduce((s, c) => s + c.width, 0))

function dateToLeft(dateStr: string | null | undefined): number {
  if (!dateStr) return -1
  const d = new Date(dateStr)
  const diffMs = d.getTime() - viewStart.value.getTime()
  const diffDays = diffMs / 86400000
  return Math.round(diffDays * dayPx.value)
}

const todayLeft = computed(() => dateToLeft(new Date().toISOString().slice(0, 10)))

// ---- 行展开 ----
function flattenTasks(list: PmTask[], depth = 0): (PmTask & { depth: number; hasChildren: boolean })[] {
  const result: (PmTask & { depth: number; hasChildren: boolean })[] = []
  for (const t of list) {
    const hasChildren = !!(t.children && t.children.length > 0)
    result.push({ ...t, depth, hasChildren })
    if (hasChildren && expandedIds.value.has(t.id)) {
      result.push(...flattenTasks(t.children!, depth + 1))
    }
  }
  return result
}

const flatRows = computed(() => flattenTasks(tasks.value))

function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) expandedIds.value.delete(id)
  else expandedIds.value.add(id)
}

// ---- 任务条样式 ----
function barClass(row: PmTask) {
  const today = new Date().toISOString().slice(0, 10)
  if (row.endDate && row.endDate < today && row.progress < 100) return 'bar-overdue'
  if (row.progress >= 50) return 'bar-blue'
  return 'bar-gray'
}

function barStyle(row: PmTask) {
  const left = dateToLeft(row.startDate)
  const rightPx = dateToLeft(row.endDate ?? row.dueDate)
  const width = Math.max(rightPx - left, 12)
  return {
    position: 'absolute' as const,
    left: left + 'px',
    width: width + 'px',
    top: '6px',
    height: (ROW_HEIGHT - 12) + 'px',
  }
}

function milestoneLeft(ms: PmMilestone): number {
  return dateToLeft(ms.dueDate)
}

// ---- 依赖线 ----
interface DepPath { key: string; d: string }
const depPaths = computed<DepPath[]>(() => {
  const paths: DepPath[] = []
  // 扁平行找行号用于 Y 定位
  const rowMap = new Map<number, number>()
  flatRows.value.forEach((r, i) => rowMap.set(r.id, i))

  // 遍历所有任务，找 dependencies (这里用 task 自身的依赖数据)
  // 实际数据中依赖信息需要后端 gantt 接口返回，此处从 tree 数据简单绘制
  // (暂时跳过：依赖数据在 projectTaskApi.dependencies 需逐个请求，生产中应由 gantt 接口合并返回)
  return paths
})

// ---- 拖拽改时间 ----
let dragState: {
  row: PmTask
  mode: 'move' | 'left' | 'right'
  startX: number
  origStart: string
  origEnd: string
} | null = null

function onBarMousedown(e: MouseEvent, row: PmTask, mode: 'move' | 'left' | 'right') {
  if (!row.startDate || !row.endDate) return
  dragState = { row, mode, startX: e.clientX, origStart: row.startDate, origEnd: row.endDate }
  window.addEventListener('mousemove', onBarMousemove)
  window.addEventListener('mouseup', onBarMouseup)
  e.preventDefault()
}

function onBarMousemove(e: MouseEvent) {
  if (!dragState) return
  const dx = e.clientX - dragState.startX
  const daysDelta = Math.round(dx / dayPx.value)
  if (daysDelta === 0) return

  const addDays = (ds: string, d: number) => {
    const dt = new Date(ds)
    dt.setDate(dt.getDate() + d)
    return dt.toISOString().slice(0, 10)
  }

  const row = dragState.row
  if (dragState.mode === 'move') {
    row.startDate = addDays(dragState.origStart, daysDelta)
    row.endDate = addDays(dragState.origEnd, daysDelta)
  } else if (dragState.mode === 'left') {
    const newStart = addDays(dragState.origStart, daysDelta)
    if (newStart < dragState.origEnd) row.startDate = newStart
  } else {
    const newEnd = addDays(dragState.origEnd, daysDelta)
    if (newEnd > dragState.origStart) row.endDate = newEnd
  }
}

function onBarMouseup() {
  if (dragState) {
    const { row } = dragState
    if (row.startDate && row.endDate) {
      updateTaskTime(row.id, { startDate: row.startDate, endDate: row.endDate }).catch(() => {
        // 还原
        if (dragState) {
          row.startDate = dragState.origStart
          row.endDate = dragState.origEnd
        }
      })
    }
    dragState = null
  }
  window.removeEventListener('mousemove', onBarMousemove)
  window.removeEventListener('mouseup', onBarMouseup)
}

// ---- 数据加载 ----
async function loadData() {
  if (!projectId.value) return
  loading.value = true
  try {
    const [taskRes, msRes] = await Promise.all([
      listTasks(projectId.value),
      listMilestones(projectId.value),
    ])
    tasks.value = (taskRes as any)?.data ?? (taskRes as any) ?? []
    milestones.value = (msRes as any)?.data ?? (msRes as any) ?? []
    // 默认展开第一层
    tasks.value.forEach(t => { if (t.children?.length) expandedIds.value.add(t.id) })
    computeViewRange(tasks.value, milestones.value)
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
    loadData()
  }
}

function scrollToToday() {
  if (!ganttRightRef.value) return
  const left = todayLeft.value - 200
  ganttRightRef.value.scrollLeft = Math.max(0, left)
}

function onScaleChange() {
  // 重新计算视图范围，保持滚动到今天
  nextTick(() => scrollToToday())
}

function syncScroll(e: Event) {
  // 左侧不需要水平滚动，忽略
}

onMounted(() => { loadProjects() })
onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onBarMousemove)
  window.removeEventListener('mouseup', onBarMouseup)
})
</script>

<style scoped>
.gantt-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: var(--n-color-modal, #fff);
}

.gantt-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--n-border-color, #e8e8e8);
  flex-shrink: 0;
}

.gantt-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.gantt-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* 左侧任务列表 */
.gantt-left {
  width: 220px;
  flex-shrink: 0;
  border-right: 1px solid var(--n-border-color, #e8e8e8);
  overflow-y: auto;
  overflow-x: hidden;
}

.gantt-left-header {
  height: 32px;
  line-height: 32px;
  padding: 0 12px;
  font-weight: 600;
  font-size: 13px;
  border-bottom: 1px solid var(--n-border-color, #e8e8e8);
  background: var(--n-color, #fafafa);
  position: sticky;
  top: 0;
  z-index: 2;
}

.gantt-row-label {
  height: 40px;
  display: flex;
  align-items: center;
  font-size: 13px;
  border-bottom: 1px solid var(--n-border-color, #f0f0f0);
  gap: 4px;
  white-space: nowrap;
  overflow: hidden;
}

.gantt-row-label.is-parent {
  font-weight: 500;
}

.gantt-row-label.milestone-label {
  color: var(--n-warning-color, #faad14);
}

.expand-icon {
  cursor: pointer;
  user-select: none;
  width: 14px;
  flex-shrink: 0;
}

.ms-icon {
  font-size: 10px;
  color: #faad14;
  flex-shrink: 0;
}

.task-name {
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 右侧时间轴 */
.gantt-right {
  flex: 1;
  overflow: auto;
  position: relative;
}

.gantt-time-header {
  display: flex;
  height: 32px;
  position: sticky;
  top: 0;
  z-index: 3;
  background: var(--n-color, #fafafa);
  border-bottom: 1px solid var(--n-border-color, #e8e8e8);
}

.time-col-header {
  flex-shrink: 0;
  height: 32px;
  line-height: 32px;
  font-size: 11px;
  text-align: center;
  border-right: 1px solid var(--n-border-color, #f0f0f0);
  overflow: hidden;
  white-space: nowrap;
}

.today-line {
  position: absolute;
  top: 32px;
  width: 2px;
  background: rgba(24, 144, 255, 0.5);
  pointer-events: none;
  z-index: 2;
}

.dep-svg {
  position: absolute;
  top: 32px;
  left: 0;
  pointer-events: none;
  z-index: 1;
}

.dep-path {
  fill: none;
  stroke: #aaa;
  stroke-width: 1.5;
  stroke-dasharray: 4 3;
}

.gantt-chart-area {
  position: relative;
  margin-top: 0;
}

.gantt-row {
  position: relative;
  border-bottom: 1px solid var(--n-border-color, #f0f0f0);
}

/* 任务条 */
.task-bar {
  border-radius: 4px;
  cursor: grab;
  user-select: none;
  overflow: hidden;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.task-bar:active { cursor: grabbing; }

.task-bar.bar-gray { background: #bdbdbd; }
.task-bar.bar-blue { background: #1890ff; }
.task-bar.bar-overdue { background: #ff4d4f; }

.bar-progress {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.25);
  pointer-events: none;
}

.bar-label {
  position: relative;
  z-index: 1;
  font-size: 11px;
  color: #fff;
  padding: 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.resize-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 8px;
  cursor: ew-resize;
  z-index: 2;
}

.resize-handle.left { left: 0; }
.resize-handle.right { right: 0; }

/* 里程碑菱形 */
.milestone-diamond {
  position: absolute;
}
</style>
