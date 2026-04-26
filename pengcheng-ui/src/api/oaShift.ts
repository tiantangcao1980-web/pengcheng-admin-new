import { request } from '@/utils/request'

/** 班次模板（attendance_shift） */
export interface ShiftItem {
  id?: number
  shiftName: string
  /** 1=固定 2=跨夜 3=弹性 */
  shiftType: number
  startTime?: string
  endTime?: string
  lateGraceMinutes?: number
  earlyGraceMinutes?: number
  minWorkMinutes?: number
  remark?: string
  enabled?: number
  createTime?: string
}

export const oaShiftApi = {
  list(params?: { enabled?: number }) {
    return request<ShiftItem[]>({ url: '/admin/oa/shifts', method: 'get', params })
  },
  detail(id: number) {
    return request<ShiftItem>({ url: `/admin/oa/shifts/${id}`, method: 'get' })
  },
  create(data: ShiftItem) {
    return request<number>({ url: '/admin/oa/shifts', method: 'post', data })
  },
  update(data: ShiftItem) {
    return request<void>({ url: `/admin/oa/shifts/${data.id}`, method: 'put', data })
  },
  remove(id: number) {
    return request<void>({ url: `/admin/oa/shifts/${id}`, method: 'delete' })
  },
}
