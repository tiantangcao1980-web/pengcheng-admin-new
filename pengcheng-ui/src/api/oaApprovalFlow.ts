import { request } from '@/utils/request'

/** 审批流程定义 */
export interface ApprovalFlowDef {
  id?: number
  bizType: string
  name: string
  enabled?: number
  isDefault?: number
  remark?: string
  createTime?: string
}

/** 审批流程节点 */
export interface ApprovalFlowNode {
  id?: number
  flowDefId?: number
  nodeOrder?: number
  nodeName: string
  /** 1=指定用户 2=部门主管 3=角色 */
  nodeType: number
  approverIds?: string
  roleKey?: string
  timeoutHours?: number
  /** 1=自动通过 2=自动驳回 3=跳过 */
  timeoutAction?: number
  allowAddSign?: number
}

/** 流程实例 */
export interface ApprovalInstance {
  id?: number
  flowDefId?: number
  bizType?: string
  bizId?: number
  applicantId?: number
  currentNodeOrder?: number
  currentNodeId?: number
  /** 1=运行 2=通过 3=驳回 4=取消 */
  state?: number
  summary?: string
  currentNodeDeadline?: string
  endTime?: string
  createTime?: string
}

export interface ApprovalRecord {
  id?: number
  instanceId?: number
  nodeId?: number
  nodeOrder?: number
  approverId?: number
  result?: number
  remark?: string
  actionTime?: string
}

export interface InstanceDetailVO {
  instance: ApprovalInstance
  records: ApprovalRecord[]
}

export interface FlowDefPayload {
  def: ApprovalFlowDef
  nodes: ApprovalFlowNode[]
}

export const oaApprovalFlowApi = {
  /** 流程定义 CRUD */
  defList(params?: { bizType?: string }) {
    return request<ApprovalFlowDef[]>({ url: '/admin/oa/approval-flow/defs', method: 'get', params })
  },
  defDetail(id: number) {
    return request<FlowDefPayload>({ url: `/admin/oa/approval-flow/defs/${id}`, method: 'get' })
  },
  defCreate(data: FlowDefPayload) {
    return request<number>({ url: '/admin/oa/approval-flow/defs', method: 'post', data })
  },
  defUpdate(data: FlowDefPayload) {
    return request<void>({ url: `/admin/oa/approval-flow/defs/${data.def.id}`, method: 'put', data })
  },
  defDelete(id: number) {
    return request<void>({ url: `/admin/oa/approval-flow/defs/${id}`, method: 'delete' })
  },

  /** 实例 */
  pending(approverId: number) {
    return request<ApprovalInstance[]>({
      url: '/admin/oa/approval-flow/instances/pending',
      method: 'get',
      params: { approverId },
    })
  },
  instanceDetail(id: number) {
    return request<InstanceDetailVO>({ url: `/admin/oa/approval-flow/instances/${id}`, method: 'get' })
  },
  handle(data: { instanceId: number; approverId: number; approved: boolean; remark?: string }) {
    return request<void>({ url: '/admin/oa/approval-flow/instances/handle', method: 'post', data })
  },
  cancel(data: { instanceId: number; applicantId: number }) {
    return request<void>({ url: '/admin/oa/approval-flow/instances/cancel', method: 'post', data })
  },
}
