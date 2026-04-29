import { request } from '@/utils/request'

// ========== TS 类型定义（J4 追加）==========
export interface PmTask {
  id: number
  projectId: number
  parentTaskId?: number | null
  title: string
  description?: string
  status: string
  priority?: number
  progress: number
  assigneeId?: number | null
  startDate?: string | null
  endDate?: string | null
  dueDate?: string | null
  estimatedHours?: number | null
  actualHours?: number | null
  tags?: string | null
  children?: PmTask[]
}

export interface PmMilestone {
  id: number
  projectId: number
  name: string
  dueDate?: string | null
  status: number
  description?: string
}

export interface PmStatusColumn {
  id: number
  projectId: number
  name: string
  statusValue: string
  sortOrder: number
  isDone: number
}

export interface PmTaskDependency {
  id: number
  taskId: number
  dependsOnTaskId: number
  dependencyType?: string
}
// ==========================================

/** 项目 */
export const projectApi = {
  list: (params: { page?: number; size?: number; scope?: string; status?: number }) =>
    request({ url: '/project/list', method: 'get', params }),
  get: (id: number) => request({ url: `/project/${id}`, method: 'get' }),
  create: (data: any) => request({ url: '/project', method: 'post', data }),
  update: (id: number, data: any) => request({ url: `/project/${id}`, method: 'put', data }),
  remove: (id: number) => request({ url: `/project/${id}`, method: 'delete' }),
  members: (id: number) => request({ url: `/project/${id}/members`, method: 'get' }),
  addMember: (id: number, userId: number, role: string) =>
    request({ url: `/project/${id}/members`, method: 'post', params: { userId, role } }),
  updateMemberRole: (id: number, userId: number, role: string) =>
    request({ url: `/project/${id}/members/${userId}`, method: 'put', params: { role } }),
  removeMember: (id: number, userId: number) =>
    request({ url: `/project/${id}/members/${userId}`, method: 'delete' }),
  stats: (id: number) => request({ url: `/project/${id}/stats`, method: 'get' }),
  /** V24 看板状态列配置 */
  statusColumns: (id: number) => request<{ data?: Array<{ id: number; name: string; statusValue: string; sortOrder: number; isDone: number }> }>({ url: `/project/${id}/status-columns`, method: 'get' }),
  createStatusColumn: (id: number, data: { name: string; statusValue: string; sortOrder?: number; isDone?: number }) =>
    request({ url: `/project/${id}/status-columns`, method: 'post', data }),
  updateStatusColumn: (columnId: number, data: { name?: string; statusValue?: string; sortOrder?: number; isDone?: number }) =>
    request({ url: `/project/status-columns/${columnId}`, method: 'put', data }),
  removeStatusColumn: (columnId: number) =>
    request({ url: `/project/status-columns/${columnId}`, method: 'delete' }),
  updateStatusColumnsOrder: (id: number, columnIds: number[]) =>
    request({ url: `/project/${id}/status-columns/order`, method: 'put', data: columnIds }),
}

/** 任务 */
export const projectTaskApi = {
  page: (projectId: number, params: any) =>
    request({ url: `/project/${projectId}/tasks`, method: 'get', params }),
  tree: (projectId: number) => request({ url: `/project/${projectId}/tasks/tree`, method: 'get' }),
  get: (taskId: number) => request({ url: `/project/task/${taskId}`, method: 'get' }),
  create: (projectId: number, data: any) =>
    request({ url: `/project/${projectId}/tasks`, method: 'post', data }),
  update: (taskId: number, data: any) =>
    request({ url: `/project/task/${taskId}`, method: 'put', data }),
  remove: (taskId: number) => request({ url: `/project/task/${taskId}`, method: 'delete' }),
  updateStatus: (taskId: number, status: string) =>
    request({ url: `/project/task/${taskId}/status`, method: 'put', params: { status } }),
  updateAssignee: (taskId: number, assigneeId: number) =>
    request({ url: `/project/task/${taskId}/assignee`, method: 'put', params: { assigneeId } }),
  updateProgress: (taskId: number, progress: number) =>
    request({ url: `/project/task/${taskId}/progress`, method: 'put', params: { progress } }),
  dependencies: (taskId: number) =>
    request({ url: `/project/task/${taskId}/dependencies`, method: 'get' }),
  addDependency: (taskId: number, dependsOnTaskId: number, type?: string) =>
    request({ url: `/project/task/${taskId}/dependencies`, method: 'post', params: { dependsOnTaskId, type } }),
  removeDependency: (taskId: number, depId: number) =>
    request({ url: `/project/task/${taskId}/dependencies/${depId}`, method: 'delete' }),
  board: (projectId: number) => request({ url: `/project/${projectId}/board`, method: 'get' }),
  gantt: (projectId: number) => request({ url: `/project/${projectId}/gantt`, method: 'get' }),
  calendar: (projectId: number) => request({ url: `/project/${projectId}/calendar`, method: 'get' }),
}

/** 里程碑 */
export const projectMilestoneApi = {
  list: (projectId: number) => request({ url: `/project/${projectId}/milestones`, method: 'get' }),
  create: (projectId: number, data: any) =>
    request({ url: `/project/${projectId}/milestones`, method: 'post', data }),
  update: (id: number, data: any) =>
    request({ url: `/project/milestone/${id}`, method: 'put', data }),
  remove: (id: number) => request({ url: `/project/milestone/${id}`, method: 'delete' }),
  setComplete: (id: number, complete: boolean) =>
    request({ url: `/project/milestone/${id}/complete`, method: 'put', params: { complete } }),
}

// ========== J4 追加：甘特图/看板专用 API（最小补丁）==========
/** 获取任务树（含依赖，用于甘特图） */
export const listTasks = (projectId: number, params?: Record<string, any>) =>
  request<{ data?: PmTask[] }>({ url: `/project/${projectId}/tasks/tree`, method: 'get', params })

/** 拖拽修改任务时间 */
export const updateTaskTime = (taskId: number, payload: { startDate: string; endDate: string }) =>
  request({ url: `/project/task/${taskId}`, method: 'put', data: payload })

/** 拖拽移动任务到看板列 */
export const updateTaskStatus = (taskId: number, columnId: string) =>
  request({ url: `/project/task/${taskId}/status`, method: 'put', params: { status: columnId } })

/** 获取看板列定义 */
export const listColumns = (projectId: number) =>
  request<{ data?: PmStatusColumn[] }>({ url: `/project/${projectId}/status-columns`, method: 'get' })

/** 获取里程碑（甘特图用） */
export const listMilestones = (projectId: number) =>
  request<{ data?: PmMilestone[] }>({ url: `/project/${projectId}/milestones`, method: 'get' })
// =============================================================
