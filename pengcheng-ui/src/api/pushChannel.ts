import { request } from '@/utils/request'

/**
 * V4.0 闭环⑤ D5 - 推送通道与订阅消息模板 API
 */

// ---------------------------------------------------------------------------
// 推送通道下发日志（push_channel_log）
// ---------------------------------------------------------------------------

export interface PushChannelLogDTO {
  id: number
  userId: number
  /** appPush / mpSubscribe / webInbox / none */
  channel: string
  bizType?: string
  bizId?: number
  title?: string
  /** 0 失败 1 成功 */
  success: number
  reason?: string
  subscribeTemplateId?: string
  createTime: string
}

export interface PushChannelLogQuery {
  userId?: number
  channel?: string
  bizType?: string
  success?: number
  startTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

export function listPushChannelLogs(query: PushChannelLogQuery) {
  return request<{ records: PushChannelLogDTO[]; total: number }>({
    url: '/system/push-channel/logs',
    method: 'get',
    params: query
  })
}

// ---------------------------------------------------------------------------
// 小程序订阅消息模板（subscribe_msg_template）
// ---------------------------------------------------------------------------

export interface SubscribeMsgTemplateDTO {
  id?: number
  bizType: string
  eventCode: string
  templateId: string
  fieldMappingJson?: string
  defaultPage?: string
  enabled: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export function listSubscribeTemplates(params?: { bizType?: string; enabled?: number }) {
  return request<SubscribeMsgTemplateDTO[]>({
    url: '/system/push-channel/subscribe-templates',
    method: 'get',
    params
  })
}

export function saveSubscribeTemplate(data: SubscribeMsgTemplateDTO) {
  return request<SubscribeMsgTemplateDTO>({
    url: '/system/push-channel/subscribe-templates',
    method: data.id ? 'put' : 'post',
    data
  })
}

export function deleteSubscribeTemplate(id: number) {
  return request<void>({
    url: `/system/push-channel/subscribe-templates/${id}`,
    method: 'delete'
  })
}

// ---------------------------------------------------------------------------
// 通道决策测试（管理员触发一次推送，验证三通道决策链）
// ---------------------------------------------------------------------------

export interface ChannelPushTestRequest {
  userId: number
  bizType: string
  bizId?: number
  title: string
  content: string
  subscribeTemplateId?: string
}

export interface ChannelPushTestResponse {
  channel: string
  success: boolean
  reason?: string
}

export function testChannelPush(data: ChannelPushTestRequest) {
  return request<ChannelPushTestResponse>({
    url: '/system/push-channel/test',
    method: 'post',
    data
  })
}
