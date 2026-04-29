<template>
  <n-drawer v-model:show="visible" :width="520" placement="right" @update:show="onVisibleChange">
    <n-drawer-content :title="task ? task.title : '任务详情'" closable>
      <n-spin :show="loading">
        <template v-if="task">
          <n-form :model="form" label-placement="left" label-width="80px" size="small">
            <!-- 标题 -->
            <n-form-item label="标题">
              <n-input v-model:value="form.title" @blur="save('title')" />
            </n-form-item>

            <!-- 描述 -->
            <n-form-item label="描述">
              <n-input
                v-model:value="form.description"
                type="textarea"
                :rows="3"
                @blur="save('description')"
              />
            </n-form-item>

            <!-- 状态 -->
            <n-form-item label="状态">
              <n-select
                v-model:value="form.status"
                :options="statusOpts"
                @update:value="save('status')"
              />
            </n-form-item>

            <!-- 优先级 -->
            <n-form-item label="优先级">
              <n-select
                v-model:value="form.priority"
                :options="priorityOpts"
                clearable
                @update:value="save('priority')"
              />
            </n-form-item>

            <!-- 负责人 -->
            <n-form-item label="负责人">
              <n-select
                v-model:value="form.assigneeId"
                :options="memberOpts"
                clearable
                filterable
                @update:value="save('assigneeId')"
              />
            </n-form-item>

            <!-- 起止时间 -->
            <n-form-item label="开始时间">
              <n-date-picker
                v-model:value="form.startDateTs"
                type="date"
                value-format="yyyy-MM-dd"
                clearable
                style="width: 100%"
                @update:value="save('startDate')"
              />
            </n-form-item>
            <n-form-item label="截止时间">
              <n-date-picker
                v-model:value="form.endDateTs"
                type="date"
                value-format="yyyy-MM-dd"
                clearable
                style="width: 100%"
                @update:value="save('endDate')"
              />
            </n-form-item>

            <!-- 进度 -->
            <n-form-item label="进度">
              <n-slider
                v-model:value="form.progress"
                :min="0"
                :max="100"
                :step="5"
                @update:value="save('progress')"
                style="flex: 1"
              />
              <span style="margin-left: 10px; white-space: nowrap; font-size: 13px">{{ form.progress }}%</span>
            </n-form-item>

            <!-- 标签 -->
            <n-form-item label="标签">
              <n-dynamic-tags v-model:value="tagList" @update:value="save('tags')" />
            </n-form-item>
          </n-form>

          <n-divider>依赖任务</n-divider>
          <div v-if="dependencies.length">
            <n-tag
              v-for="dep in dependencies"
              :key="dep.id"
              type="info"
              closable
              @close="removeDep(dep)"
              style="margin: 2px"
            >
              #{{ dep.dependsOnTaskId }}
            </n-tag>
          </div>
          <n-empty v-else description="暂无依赖" size="small" />

          <n-divider>子任务</n-divider>
          <div v-if="subTasks.length">
            <div v-for="st in subTasks" :key="st.id" class="subtask-row">
              <n-checkbox :checked="st.progress === 100" @update:checked="toggleSubTask(st)" />
              <span :class="{ done: st.progress === 100 }">{{ st.title }}</span>
            </div>
          </div>
          <n-empty v-else description="暂无子任务" size="small" />

          <n-divider />
          <n-space justify="end">
            <n-button size="small" type="error" quaternary @click="deleteTask">删除任务</n-button>
          </n-space>
        </template>
      </n-spin>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import {
  NDrawer, NDrawerContent, NSpin, NForm, NFormItem, NInput, NSelect,
  NDatePicker, NSlider, NDynamicTags, NDivider, NTag, NEmpty,
  NCheckbox, NButton, NSpace, useMessage,
} from 'naive-ui'
import { projectTaskApi } from '@/api/project'
import type { PmTask, PmTaskDependency } from '@/api/project'

const props = defineProps<{
  show: boolean
  taskId: number | null
}>()

const emit = defineEmits<{
  'update:show': [val: boolean]
  saved: []
}>()

const message = useMessage()
const loading = ref(false)
const task = ref<PmTask | null>(null)
const dependencies = ref<PmTaskDependency[]>([])
const subTasks = ref<PmTask[]>([])
const memberOpts = ref<{ label: string; value: number }[]>([])

const visible = computed({
  get: () => props.show,
  set: (v) => emit('update:show', v),
})

const form = reactive({
  title: '',
  description: '',
  status: '',
  priority: null as number | null,
  assigneeId: null as number | null,
  startDateTs: null as number | null,
  endDateTs: null as number | null,
  progress: 0,
  tags: '',
})

const tagList = computed({
  get: () => form.tags ? form.tags.split(',').filter(Boolean) : [],
  set: (v: string[]) => { form.tags = v.join(',') },
})

const statusOpts = [
  { label: '待办', value: '待办' },
  { label: '进行中', value: '进行中' },
  { label: '已完成', value: '已完成' },
  { label: '已取消', value: '已取消' },
]

const priorityOpts = [
  { label: '低', value: 1 },
  { label: '中', value: 2 },
  { label: '高', value: 3 },
  { label: '紧急', value: 4 },
]

function fillForm(t: PmTask) {
  form.title = t.title ?? ''
  form.description = t.description ?? ''
  form.status = t.status ?? ''
  form.priority = t.priority ?? null
  form.assigneeId = t.assigneeId ?? null
  form.startDateTs = t.startDate ? new Date(t.startDate).getTime() : null
  form.endDateTs = t.endDate ? new Date(t.endDate).getTime() : null
  form.progress = t.progress ?? 0
  form.tags = t.tags ?? ''
}

async function loadTask() {
  if (!props.taskId) return
  loading.value = true
  try {
    const [taskRes, depRes] = await Promise.all([
      projectTaskApi.get(props.taskId),
      projectTaskApi.dependencies(props.taskId),
    ])
    task.value = (taskRes as any)?.data ?? taskRes
    if (task.value) fillForm(task.value)
    const rawDeps = (depRes as any)?.data ?? depRes ?? []
    dependencies.value = Array.isArray(rawDeps) ? rawDeps : []

    // 子任务：从 task 的 children
    subTasks.value = task.value?.children ?? []
  } catch {
    message.error('加载任务失败')
  } finally {
    loading.value = false
  }
}

let saveTimer: ReturnType<typeof setTimeout> | null = null

async function save(field: string) {
  if (!props.taskId || !task.value) return
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(async () => {
    const payload: Record<string, any> = {}
    if (field === 'title') payload.title = form.title
    else if (field === 'description') payload.description = form.description
    else if (field === 'status') payload.status = form.status
    else if (field === 'priority') payload.priority = form.priority
    else if (field === 'assigneeId') payload.assigneeId = form.assigneeId
    else if (field === 'startDate') payload.startDate = form.startDateTs ? new Date(form.startDateTs).toISOString().slice(0, 10) : null
    else if (field === 'endDate') payload.endDate = form.endDateTs ? new Date(form.endDateTs).toISOString().slice(0, 10) : null
    else if (field === 'progress') payload.progress = form.progress
    else if (field === 'tags') payload.tags = form.tags
    try {
      await projectTaskApi.update(props.taskId!, payload)
      emit('saved')
    } catch {
      message.error('保存失败')
    }
  }, 600)
}

async function removeDep(dep: PmTaskDependency) {
  if (!props.taskId) return
  try {
    await projectTaskApi.removeDependency(props.taskId, dep.id)
    dependencies.value = dependencies.value.filter(d => d.id !== dep.id)
  } catch {
    message.error('删除依赖失败')
  }
}

async function toggleSubTask(st: PmTask) {
  const newProgress = st.progress === 100 ? 0 : 100
  try {
    await projectTaskApi.update(st.id, { progress: newProgress })
    st.progress = newProgress
    emit('saved')
  } catch {
    message.error('更新失败')
  }
}

async function deleteTask() {
  if (!props.taskId) return
  if (!window.confirm('确定删除该任务？')) return
  try {
    await projectTaskApi.remove(props.taskId)
    message.success('已删除')
    emit('update:show', false)
    emit('saved')
  } catch {
    message.error('删除失败')
  }
}

function onVisibleChange(v: boolean) {
  if (!v && saveTimer) {
    clearTimeout(saveTimer)
    saveTimer = null
  }
}

watch(() => props.show, (v) => {
  if (v && props.taskId) loadTask()
})
</script>

<style scoped>
.subtask-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
}

.subtask-row .done {
  text-decoration: line-through;
  color: var(--n-text-color-3, #999);
}
</style>
