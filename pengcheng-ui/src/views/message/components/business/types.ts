/**
 * 业务消息载荷类型
 *
 * 与后端 BusinessMessagePayload.java 对齐：
 *   - businessType 决定卡片业务语义
 *   - data 字段结构按 type 不同而异
 *
 * 6 种类型：CARD / LOCATION / GOODS / FORM / CHOICE / EVENT
 */

export type BusinessMessageType =
  | 'CARD'
  | 'LOCATION'
  | 'GOODS'
  | 'FORM'
  | 'CHOICE'
  | 'EVENT'

/** 客户名片 */
export interface CardPayloadData {
  customerId: number
  customerName: string
  phoneMasked?: string
  dealProbability?: number
}

/** 位置 */
export interface LocationPayloadData {
  lng: number
  lat: number
  address?: string
  customerId?: number
}

/** 楼盘卡片 */
export interface GoodsPayloadData {
  projectId: number
  projectName: string
  price?: number | string
  area?: number | string
  image?: string
}

/** 表单字段定义 */
export interface FormFieldSpec {
  name: string
  label: string
  type?: string
  required?: boolean
}

/** 表单 */
export interface FormPayloadData {
  formId?: string | number
  title: string
  fields: FormFieldSpec[]
}

/** 选项 */
export interface ChoiceOption {
  value: string | number
  label: string
}

/** 快速选项 */
export interface ChoicePayloadData {
  question: string
  options: ChoiceOption[]
}

/** 系统事件 */
export interface EventPayloadData {
  eventCode: string
  actorId?: number
  actorName?: string
  message: string
}

/** 业务消息载荷（后端 toJson 后的 wrapper 格式） */
export interface BusinessMessagePayload {
  businessType: BusinessMessageType
  data:
    | CardPayloadData
    | LocationPayloadData
    | GoodsPayloadData
    | FormPayloadData
    | ChoicePayloadData
    | EventPayloadData
    | Record<string, unknown>
}
