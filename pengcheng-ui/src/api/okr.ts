import { request } from '@/utils/request'

// ==================== 类型定义 ====================

export interface OkrPeriod {
  id?: number
  code: string
  name: string
  startDate: string
  endDate: string
  status?: number
  createTime?: string
}

export interface OkrObjective {
  id?: number
  periodId: number
  ownerId: number
  ownerType?: string
  parentId?: number
  title: string
  description?: string
  progress?: number
  weight?: number
  status?: number
  createTime?: string
  updateTime?: string
}

export interface OkrKeyResult {
  id?: number
  objectiveId: number
  title: string
  measureType: string
  targetValue?: number
  currentValue?: number
  unit?: string
  progress?: number
  weight?: number
  status?: number
}

export interface OkrCheckin {
  id?: number
  objectiveId: number
  keyResultId?: number
  userId: number
  weekIndex: number
  progress: number
  confidence?: number
  summary?: string
  issues?: string
  nextSteps?: string
  createTime?: string
}

export interface CreateObjectiveDTO {
  periodId: number
  ownerId: number
  ownerType?: string
  parentId?: number
  title: string
  description?: string
  weight?: number
}

export interface UpdateProgressDTO {
  keyResultId: number
  currentValue?: number
  progress?: number
}

export interface CheckinDTO {
  objectiveId: number
  keyResultId?: number
  userId: number
  weekIndex: number
  progress: number
  confidence?: number
  summary?: string
  issues?: string
  nextSteps?: string
}

// ==================== 周期 API ====================

export const okrPeriodApi = {
  listAll: () =>
    request<OkrPeriod[]>({ url: '/admin/hr/okr/periods', method: 'get' }),

  listActive: () =>
    request<OkrPeriod[]>({ url: '/admin/hr/okr/periods/active', method: 'get' }),

  create: (data: OkrPeriod) =>
    request<number>({ url: '/admin/hr/okr/periods', method: 'post', data }),

  activate: (id: number) =>
    request<void>({ url: `/admin/hr/okr/periods/${id}/activate`, method: 'put' }),

  close: (id: number) =>
    request<void>({ url: `/admin/hr/okr/periods/${id}/close`, method: 'put' }),
}

// ==================== 目标 API ====================

export const okrObjectiveApi = {
  list: (params: { ownerId: number; ownerType?: string; periodId?: number }) =>
    request<OkrObjective[]>({ url: '/admin/hr/okr/objectives', method: 'get', params }),

  listTree: (params: { periodId?: number; parentId?: number }) =>
    request<OkrObjective[]>({ url: '/admin/hr/okr/objectives/tree', method: 'get', params }),

  create: (data: CreateObjectiveDTO) =>
    request<number>({ url: '/admin/hr/okr/objectives', method: 'post', data }),

  update: (data: Partial<OkrObjective> & { id: number }) =>
    request<void>({ url: '/admin/hr/okr/objectives', method: 'put', data }),

  delete: (id: number) =>
    request<void>({ url: `/admin/hr/okr/objectives/${id}`, method: 'delete' }),

  suggestKr: (id: number, title?: string, description?: string) =>
    request<string[]>({
      url: `/admin/hr/okr/objectives/${id}/suggest-key-results`,
      method: 'post',
      params: { title, description },
    }),
}

// ==================== 关键结果 API ====================

export const okrKeyResultApi = {
  list: (objectiveId: number) =>
    request<OkrKeyResult[]>({
      url: '/admin/hr/okr/key-results',
      method: 'get',
      params: { objectiveId },
    }),

  create: (data: OkrKeyResult) =>
    request<number>({ url: '/admin/hr/okr/key-results', method: 'post', data }),

  update: (data: Partial<OkrKeyResult> & { id: number }) =>
    request<void>({ url: '/admin/hr/okr/key-results', method: 'put', data }),

  updateProgress: (data: UpdateProgressDTO) =>
    request<void>({ url: '/admin/hr/okr/key-results/progress', method: 'put', data }),
}

// ==================== Check-in API ====================

export const okrCheckinApi = {
  submit: (data: CheckinDTO) =>
    request<number>({ url: '/admin/hr/okr/checkins', method: 'post', data }),

  listByObjective: (objectiveId: number) =>
    request<OkrCheckin[]>({
      url: '/admin/hr/okr/checkins',
      method: 'get',
      params: { objectiveId },
    }),

  listByUserPeriod: (userId: number, periodId: number) =>
    request<OkrCheckin[]>({
      url: '/admin/hr/okr/checkins/user',
      method: 'get',
      params: { userId, periodId },
    }),
}
