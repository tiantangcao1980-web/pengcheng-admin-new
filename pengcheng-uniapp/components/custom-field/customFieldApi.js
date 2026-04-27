/**
 * 自定义字段 API — uniapp 端
 * 对齐后端路径：/api/crm/custom-fields
 */
import { get, put } from '../../utils/request.js'

/**
 * 获取字段定义列表
 * @param {string} entityType  - 'customer' | 'lead' | 'opportunity'
 */
export const listDefs = (entityType) =>
  get('/api/crm/custom-fields/defs', { entityType })

/**
 * 获取实体的字段值 Map
 * @param {string} entityType
 * @param {number|string} entityId
 */
export const getValues = (entityType, entityId) =>
  get('/api/crm/custom-fields/values', { entityType, entityId })

/**
 * 批量保存字段值
 * @param {string} entityType
 * @param {number|string} entityId
 * @param {Record<string, any>} values
 */
export const saveValues = (entityType, entityId, values) =>
  put(`/api/crm/custom-fields/values?entityType=${encodeURIComponent(entityType)}&entityId=${entityId}`, values)
