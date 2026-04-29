import { request } from '@/utils/request'

// ---------- 类型定义 ----------

export interface HouseType {
  id?: number
  projectId: number
  code: string
  name: string
  bedrooms?: number
  livingRooms?: number
  bathrooms?: number
  area: number
  insideArea?: number
  orientation?: string
  layoutImage?: string
  basePrice?: number
  description?: string
  enabled?: number
  createTime?: string
}

export interface RealtyUnit {
  id?: number
  projectId: number
  houseTypeId: number
  building: string
  floor: number
  unitNo: string
  fullNo?: string
  area: number
  listPrice: number
  actualPrice?: number
  status?: string
  lockedBy?: number
  lockedUntil?: string
  customerId?: number
  dealId?: number
  remark?: string
  createTime?: string
}

export type UnitStatus = 'AVAILABLE' | 'RESERVED' | 'SUBSCRIBED' | 'SIGNED' | 'SOLD' | 'UNAVAILABLE'

export interface FloorRow {
  floor: number
  units: RealtyUnit[]
}

export interface UnitMatrix {
  building: string
  floors: FloorRow[]
}

export interface StatusChangeRequest {
  toStatus: UnitStatus
  operatorId?: number
  customerId?: number
  dealId?: number
  reason?: string
}

export interface LockRequest {
  userId: number
  hours?: number
}

// ---------- 户型 API ----------

export const houseTypeApi = {
  /** 按楼盘查询所有户型 */
  listByProject: (projectId: number) =>
    request.get<HouseType[]>('/admin/realty/house-types/by-project', { params: { projectId } }),

  /** 按楼盘查询启用户型 */
  listEnabled: (projectId: number) =>
    request.get<HouseType[]>('/admin/realty/house-types/enabled', { params: { projectId } }),

  /** 获取户型详情 */
  detail: (id: number) =>
    request.get<HouseType>(`/admin/realty/house-types/${id}`),

  /** 创建户型 */
  create: (data: HouseType) =>
    request.post<number>('/admin/realty/house-types', data),

  /** 编辑户型 */
  update: (id: number, data: HouseType) =>
    request.put<void>(`/admin/realty/house-types/${id}`, data),

  /** 删除户型 */
  delete: (id: number) =>
    request.delete<void>(`/admin/realty/house-types/${id}`),
}

// ---------- 房源 API ----------

export const unitApi = {
  /** 房源状态矩阵（楼栋 × 楼层 × 房间） */
  matrix: (projectId: number) =>
    request.get<UnitMatrix[]>('/admin/realty/units/matrix', { params: { projectId } }),

  /** 按状态筛选房源 */
  byStatus: (projectId: number, status?: UnitStatus) =>
    request.get<RealtyUnit[]>('/admin/realty/units/by-status', { params: { projectId, status } }),

  /** 获取房源详情 */
  detail: (id: number) =>
    request.get<RealtyUnit>(`/admin/realty/units/${id}`),

  /** 创建房源 */
  create: (data: RealtyUnit) =>
    request.post<number>('/admin/realty/units', data),

  /** 编辑房源 */
  update: (id: number, data: RealtyUnit) =>
    request.put<void>(`/admin/realty/units/${id}`, data),

  /** 删除房源 */
  delete: (id: number) =>
    request.delete<void>(`/admin/realty/units/${id}`),

  /** 变更房源状态 */
  changeStatus: (id: number, req: StatusChangeRequest) =>
    request.post<void>(`/admin/realty/units/${id}/change-status`, req),

  /** 原子锁定（返回 true 成功） */
  lock: (id: number, req: LockRequest) =>
    request.post<boolean>(`/admin/realty/units/${id}/lock`, req),

  /** 解锁 */
  unlock: (id: number) =>
    request.post<void>(`/admin/realty/units/${id}/unlock`),
}
