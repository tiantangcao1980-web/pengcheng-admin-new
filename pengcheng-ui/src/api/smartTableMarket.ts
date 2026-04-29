import { request } from '@/utils/request'

/** 市场模板完整类型 */
export interface MarketTemplate {
  id: number
  name: string
  description: string
  category: string
  icon: string
  coverUrl: string
  authorName: string
  tags: string
  shareStatus: 'PRIVATE' | 'REVIEWING' | 'PUBLIC' | 'REJECTED'
  builtIn: boolean
  downloadCount: number
  ratingCount: number
  ratingSum: number
  avgRating: number
  fieldsConfig: FieldDef[]
  createdAt?: string
}

export interface FieldDef {
  name: string
  field_key: string
  field_type: string
  required?: boolean
}

export interface RatingRecord {
  id: number
  userId: number
  rating: number
  review: string
  createdAt: string
}

/** 市场列表查询参数 */
export interface MarketListParams {
  category?: string
  keyword?: string
  sort?: 'downloads' | 'rating' | 'latest'
  page?: number
  size?: number
}

/** 分享请求参数 */
export interface ShareParams {
  authorName: string
  tags?: string
}

/** 评分请求参数 */
export interface RateParams {
  rating: number
  review?: string
}

export const smartTableMarketApi = {
  /** GET 市场列表（PUBLIC + 内置模板） */
  listMarket(params: MarketListParams = {}) {
    return request<any>({
      url: '/admin/smarttable/template-market',
      method: 'get',
      params: {
        category: params.category || undefined,
        keyword: params.keyword || undefined,
        sort: params.sort || 'downloads',
        page: params.page || 1,
        size: params.size || 20
      }
    })
  },

  /** POST /{id}/share — 分享私有模板到市场（进入审核） */
  shareTemplate(id: number, data: ShareParams) {
    return request<void>({
      url: `/admin/smarttable/template-market/${id}/share`,
      method: 'post',
      data
    })
  },

  /** POST /{id}/review — 管理员审核（通过/拒绝） */
  reviewTemplate(id: number, approve: boolean) {
    return request<void>({
      url: `/admin/smarttable/template-market/${id}/review`,
      method: 'post',
      params: { approve }
    })
  },

  /** POST /{id}/download — 下载模板回调 */
  downloadTemplate(id: number, targetTableId?: number) {
    return request<void>({
      url: `/admin/smarttable/template-market/${id}/download`,
      method: 'post',
      params: targetTableId ? { targetTableId } : {}
    })
  },

  /** POST /{id}/rate — 评分（1-5 + 可选短评） */
  rateTemplate(id: number, data: RateParams) {
    return request<void>({
      url: `/admin/smarttable/template-market/${id}/rate`,
      method: 'post',
      data
    })
  }
}
