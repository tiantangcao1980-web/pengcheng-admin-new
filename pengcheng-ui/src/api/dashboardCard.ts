import { request } from '@/utils/request'

// ─── 类型定义 ───────────────────────────────────────────────────────────────

export interface CardMeta {
  code: string
  name: string
  category: string
  suggestedChart: string
  defaultCols: number
  defaultRows: number
  description?: string
  applicableRoles?: string[]
}

export interface LayoutItem {
  cardCode: string
  x: number
  y: number
  w: number
  h: number
}

export interface Layout {
  id?: number
  ownerType: 'user' | 'role' | 'global'
  ownerId?: string | number
  name?: string
  layoutJson: LayoutItem[]
  isDefault?: boolean
}

export interface RenderCardBody {
  windowStart?: string
  windowEnd?: string
  [key: string]: unknown
}

// ─── API 函数 ────────────────────────────────────────────────────────────────

/** 获取所有可用卡片元数据 */
export function listCards(): Promise<CardMeta[]> {
  return request({ url: '/admin/dashboard/cards', method: 'get' })
}

/** 渲染指定卡片数据 */
export function renderCard(code: string, body: RenderCardBody = {}): Promise<unknown> {
  return request({
    url: `/admin/dashboard/cards/${code}/render`,
    method: 'post',
    data: body
  })
}

/** 获取默认布局 */
export function getDefaultLayout(params: {
  ownerType: 'user' | 'role' | 'global'
  ownerId?: string | number
}): Promise<Layout> {
  return request({
    url: '/admin/dashboard/layouts/default',
    method: 'get',
    params
  })
}

/** 保存（创建或更新）布局 */
export function saveLayout(layout: Layout): Promise<Layout> {
  return request({
    url: '/admin/dashboard/layouts',
    method: 'put',
    data: layout
  })
}

/** 获取布局列表 */
export function listLayouts(params: {
  ownerType?: 'user' | 'role' | 'global'
  ownerId?: string | number
}): Promise<Layout[]> {
  return request({
    url: '/admin/dashboard/layouts',
    method: 'get',
    params
  })
}
