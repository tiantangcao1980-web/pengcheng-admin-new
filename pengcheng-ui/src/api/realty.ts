import { request } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface ProjectOption {
  id: number
  projectName: string
}

export interface AllianceOption {
  id: number
  companyName: string
  status?: number
}

/** 客户性别：M-男 F-女 O-其他（V17 新增） */
export type CustomerGender = 'M' | 'F' | 'O'

export const CUSTOMER_GENDER_LABEL: Record<CustomerGender, string> = {
  M: '男',
  F: '女',
  O: '其他'
}

export const CUSTOMER_GENDER_OPTIONS: Array<{ label: string; value: CustomerGender }> = [
  { label: '男', value: 'M' },
  { label: '女', value: 'F' },
  { label: '其他', value: 'O' }
]

/** 客户报备状态枚举（V1.0 对齐：1 已报备 2 已到访 3 已成交 4 已流失 5 已退订） */
export const CUSTOMER_STATUS_LABEL: Record<number, string> = {
  1: '已报备',
  2: '已到访',
  3: '已成交',
  4: '已流失',
  5: '已退订'
} as const

export const CUSTOMER_STATUS_TAG_TYPE: Record<number, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
  1: 'default',
  2: 'info',
  3: 'success',
  4: 'error',
  5: 'warning'
}

export const CUSTOMER_STATUS_OPTIONS = [
  { label: '已报备', value: 1 },
  { label: '已到访', value: 2 },
  { label: '已成交', value: 3 },
  { label: '已流失', value: 4 },
  { label: '已退订', value: 5 }
]

export interface CustomerRecord {
  id: number
  reportNo?: string
  customerName?: string
  /** 性别（V17 新增） */
  gender?: CustomerGender
  phoneMasked?: string
  visitCount?: number
  visitTime?: string
  allianceId?: number
  allianceName?: string
  agentName?: string
  agentPhoneMasked?: string
  status?: number
  poolType?: number
  protectionExpireTime?: string
  lastFollowTime?: string
  dealProbability?: number
  createTime?: string
}

export interface CustomerQueryParams {
  page: number
  pageSize: number
  customerName?: string
  projectId?: number
  allianceId?: number
  status?: number
  startTime?: string
  endTime?: string
}

export interface CustomerCreateParams {
  projectIds: number[]
  customerName: string
  /** 性别（V17 新增，可选） */
  gender?: CustomerGender
  phone: string
  visitCount: number
  visitTime: string
  allianceId: number
  agentName: string
  agentPhone: string
}

export interface CustomerCreateResult {
  customerId: number
  reportNo: string
  hasDuplicate?: boolean
  analysisMessage?: string
}

/** 拜访录入用户类型：1=联盟商 2=开发商（V17 新增） */
export const VISIT_USER_TYPE_ALLIANCE = 1
export const VISIT_USER_TYPE_DEVELOPER = 2

export const VISIT_USER_TYPE_OPTIONS = [
  { label: '联盟商', value: VISIT_USER_TYPE_ALLIANCE },
  { label: '开发商', value: VISIT_USER_TYPE_DEVELOPER }
]

export interface CustomerVisitRecord {
  id: number
  customerId: number
  /** 实际到访时间（旧字段，保留） */
  actualVisitTime: string
  /** 带看日期（V17 新增，必填，yyyy-MM-dd） */
  visitDate?: string
  /** 带看时间（V17 新增，选填，HH:mm 或 HH:mm:ss） */
  visitTimeOnly?: string
  /** 带看公司名称（V17 新增） */
  visitCompany?: string
  /** 用户类型：1=联盟商 2=开发商（V17 新增） */
  userType?: number
  /** 关联联盟商或开发商 ID，按 userType 路由（V17 新增） */
  partnerId?: number
  actualVisitCount: number
  receptionist: string
  remark?: string
  createTime?: string
}

/** 客户到访录入参数（V17 字段） */
export interface CustomerVisitCreateParams {
  customerId: number
  /** 带看日期（必填） */
  visitDate: string
  /** 带看时间（选填） */
  visitTimeOnly?: string
  /** 带看公司名称 */
  visitCompany?: string
  /** 用户类型：1=联盟商 2=开发商 */
  userType: number
  /** 关联联盟商/开发商 ID */
  partnerId?: number
  /** 实际到访人数 */
  actualVisitCount?: number
  /** 接待人员 */
  receptionist?: string
  remark?: string
}

export interface CustomerDealRecord {
  id: number
  customerId: number
  roomNo: string
  dealAmount: number
  dealTime: string
  signStatus: number
  subscribeType: number
  onlineSignStatus: number
  filingStatus: number
  loanStatus: number
  paymentStatus: number
  createTime?: string
}

export interface AllianceRecord {
  id: number
  companyName: string
  officeAddress?: string
  contactName?: string
  contactPhone?: string
  staffSize?: number
  level?: number
  status?: number
  userId?: number
  channelUserId?: number
  createTime?: string
  updateTime?: string
}

export interface AllianceQueryParams {
  page: number
  pageSize: number
  companyName?: string
  status?: number
  level?: number
}

export interface AllianceCreateParams {
  id?: number
  companyName: string
  officeAddress: string
  contactName: string
  contactPhone: string
  staffSize: number
  level: number
  channelUserId?: number
}

export interface AllianceStatsRecord {
  allianceId: number
  companyName: string
  customerCount?: number
  dealCount?: number
  dealAmount?: number
  settledCommission?: number
  pendingCommission?: number
  transportExpense?: number
  promotionExpense?: number
  entertainmentExpense?: number
  channelVisitCount?: number
}

export interface CommissionDetailRecord {
  baseCommission?: number
  jumpPointCommission?: number
  cashReward?: number
  firstDealReward?: number
  platformReward?: number
}

export interface CommissionRecord {
  id: number
  dealId: number
  projectId: number
  allianceId: number
  receivableAmount: number
  payableAmount: number
  platformFee: number
  auditStatus: number
  auditRemark?: string
  auditorId?: number
  auditTime?: string
  createTime?: string
  detail?: CommissionDetailRecord
}

export interface CommissionQueryParams {
  page: number
  pageSize: number
  projectId?: number
  allianceId?: number
  auditStatus?: number
}

export interface CommissionCreateParams {
  dealId: number
  projectId: number
  allianceId: number
  receivableAmount: number
  payableAmount: number
  platformFee: number
  detail?: CommissionDetailRecord
}

export type CommissionApprovalNode =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'MANAGER_APPROVED'
  | 'FINANCE_APPROVED'
  | 'PAID'
  | 'REJECTED'

export const COMMISSION_NODE_LABEL: Record<CommissionApprovalNode, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交（待主管审批）',
  MANAGER_APPROVED: '主管已审（待财务审批）',
  FINANCE_APPROVED: '财务已审（待放款）',
  PAID: '已放款',
  REJECTED: '已驳回'
}

export interface CommissionSubmitParams {
  commissionId: number
  submitterId?: number
  remark?: string
}

export interface CommissionApprovalActionParams {
  commissionId: number
  approverId?: number
  approved: boolean
  remark?: string
}

export interface CommissionApprovalRecord {
  id: number
  commissionId: number
  /** MANAGER / FINANCE / PAYMENT */
  node: string
  approverId: number
  /** 1=通过 2=驳回 */
  result: number
  remark?: string
  /** 1=主管 2=财务 3=放款 */
  approvalOrder: number
  approvalTime?: string
}

// ---------- 销售漏斗 / 商机 ----------
export interface PipelineStageRecord {
  id: number
  name: string
  /** LEAD / INTENT / VISIT / SUBSCRIBE / SIGNED / LOST */
  code: string
  orderNo: number
  winRate: number
  color?: string
  isTerminal: number
  active: number
}

export interface OpportunityRecord {
  id: number
  customerId: number
  projectId: number
  stageId: number
  title?: string
  expectedAmount?: number
  expectedCloseDate?: string
  ownerId?: number
  nextAction?: string
  nextActionAt?: string
  lostReason?: string
  lastStageChangedAt?: string
  createTime?: string
  updateTime?: string
}

export interface OpportunityCreateParams {
  customerId: number
  projectId: number
  stageId?: number
  title?: string
  expectedAmount?: number
  expectedCloseDate?: string
  ownerId?: number
  nextAction?: string
}

export interface OpportunityMoveStageParams {
  opportunityId: number
  toStageId: number
  operatorId?: number
  remark?: string
}

export interface OpportunityStageLogRecord {
  id: number
  opportunityId: number
  fromStageId?: number
  toStageId: number
  operatorId?: number
  remark?: string
  changeTime?: string
}

// ---------- 报表文件类型 ----------
export type ReportFileType =
  | 'sales-performance'
  | 'customer-analysis'
  | 'commission-list'
  | 'customer-followup-report'

export const REPORT_FILE_TYPE_LABEL: Record<ReportFileType, string> = {
  'sales-performance': '销售业绩报表',
  'customer-analysis': '客户分析报表',
  'commission-list': '佣金明细报表',
  'customer-followup-report': '客户跟进报表'
}

// ---------- 工单（IT/HR 内部流转） ----------
export type TicketStatus =
  | 'CREATED'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'CLOSED'
  | 'CANCELLED'

export const TICKET_STATUS_LABEL: Record<TicketStatus, string> = {
  CREATED: '待分配',
  ASSIGNED: '已分配',
  IN_PROGRESS: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
  CANCELLED: '已取消'
}

export type TicketCategory = 'IT' | 'HR' | 'FINANCE' | 'OTHER'

export interface TicketCreateParams {
  title: string
  content?: string
  category: TicketCategory | string
  /** 1低 2中 3高 4紧急；不填默认 2 */
  priority?: number
  /** 提单人；可选，后端会从 Sa-Token 注入 */
  submitterId?: number
}

export interface TicketRecord {
  id: number
  ticketNo?: string
  title: string
  content?: string
  category: string
  priority: number
  status: TicketStatus | string
  submitterId?: number
  assigneeId?: number
  resolvedAt?: string
  closedAt?: string
  extra?: string
  createTime?: string
  updateTime?: string
}

export interface TicketLogRecord {
  id: number
  ticketId: number
  /** CREATE / ASSIGN / REPLY / RESOLVE / CLOSE / CANCEL / REOPEN */
  action: string
  fromStatus?: string
  toStatus?: string
  operatorId?: number
  content?: string
  createTime?: string
}

export interface CommissionAuditParams {
  commissionId: number
  approved: boolean
  remark?: string
  auditorId?: number
}

export interface CommissionChangeLogRecord {
  id: number
  commissionId: number
  fieldName: string
  oldValue?: string
  newValue?: string
  operatorId?: number
  changeTime?: string
}

export interface AttendanceRecordItem {
  id: number
  userId: number
  attendanceDate: string
  clockInTime?: string
  clockInLocation?: string
  clockInStatus?: number
  clockOutTime?: string
  clockOutLocation?: string
  clockOutStatus?: number
}

export interface LeaveRequestItem {
  id: number
  userId: number
  leaveType: number
  startTime: string
  endTime: string
  reason?: string
  status: number
  createTime?: string
}

export interface CompensateRequestItem {
  id: number
  userId: number
  compensateDate: string
  reason?: string
  status: number
  createTime?: string
}

export interface AttendanceMonthlyRecord {
  userId: number
  year: number
  month: number
  attendanceDays: number
  lateTimes: number
  earlyLeaveTimes: number
  leaveDays: number
  overtimeHours: number
}

export interface PaymentRecord {
  id: number
  applicantId: number
  requestType: number
  expenseType?: number
  amount: number
  description?: string
  relatedDealId?: number
  relatedAllianceId?: number
  attachments?: string
  status: number
  createTime?: string
  approvals?: PaymentApprovalRecord[]
}

export interface PaymentApprovalRecord {
  id: number
  requestId: number
  approverId: number
  result: number
  remark?: string
  approvalOrder?: number
  approvalTime?: string
  createTime?: string
  updateTime?: string
}

export interface PaymentQueryParams {
  page: number
  pageSize: number
  requestType?: number
  status?: number
  applicantId?: number
}

export interface PaymentCreateParams {
  applicantId: number
  requestType: number
  expenseType?: number
  amount: number
  description?: string
  relatedDealId?: number
  relatedAllianceId?: number
  attachments?: string
  occurTime?: string
}

export interface PaymentApproveParams {
  requestId: number
  approverId: number
  approved: boolean
  remark?: string
}

export interface ProjectRecord {
  id: number
  projectName: string
  developerName?: string
  address?: string
  projectType?: number
  status?: number
  district?: string
  agencyStartDate?: string
  agencyEndDate?: string
  contactPerson?: string
  contactPhone?: string
  description?: string
  createTime?: string
  updateTime?: string
  commissionRule?: ProjectCommissionRuleRecord
}

export interface ProjectQueryParams {
  page: number
  pageSize: number
  projectName?: string
  district?: string
  projectType?: number
  status?: number
}

export interface ProjectCreateParams {
  id?: number
  projectName: string
  developerName?: string
  address?: string
  projectType?: number
  status?: number
  district?: string
  agencyStartDate?: string
  agencyEndDate?: string
  contactPerson?: string
  contactPhone?: string
  description?: string
}

export interface ProjectCommissionRuleCreateParams {
  projectId: number
  baseRate?: number
  jumpPointRules?: string
  cashReward?: number
  firstDealReward?: number
  platformReward?: number
}

export interface ProjectCommissionRuleRecord {
  id: number
  projectId: number
  baseRate?: number
  jumpPointRules?: string
  cashReward?: number
  firstDealReward?: number
  platformReward?: number
  version?: number
  status?: number
  createTime?: string
}

export interface DashboardOverviewRecord {
  reportCount: number
  visitCount: number
  dealCount: number
  dealAmount: number
  receivableCommission: number
  settledCommission: number
}

export interface DashboardFunnelRecord {
  reportCount: number
  visitCount: number
  dealCount: number
  reportToVisitRate: number
  visitToDealRate: number
  reportToDealRate: number
}

export interface DashboardRankingRecord {
  projectRanking: Array<{
    projectId: number
    projectName: string
    dealCount: number
    dealAmount: number
  }>
  allianceRanking: Array<{
    allianceId: number
    companyName: string
    customerCount: number
    dealCount: number
  }>
}

export interface AiChatRecord {
  content: string
  displayType: 'table' | 'chart' | 'text'
  conversationId?: string
  routedAgent?: string
  structuredData?: Record<string, unknown>
}

export interface AiKnowledgeDocRecord {
  id: number
  fileName?: string
  projectId?: number
  status?: string
  uploadTime?: string
}

// 房产业务真实后端接口（交付标准：全部对接 /admin/customer、/admin/project、/admin/alliance、/admin/commission、/admin/payment、/admin/dashboard）
export const realtyApi = {
  // ---------- 客户 ----------
  customerPage(params: CustomerQueryParams) {
    return request<PageResult<CustomerRecord>>({
      url: '/admin/customer/page',
      method: 'get',
      params: {
        page: params.page,
        pageSize: params.pageSize,
        customerName: params.customerName,
        projectId: params.projectId,
        allianceId: params.allianceId,
        status: params.status,
        startTime: params.startTime,
        endTime: params.endTime
      }
    })
  },

  createCustomer(data: CustomerCreateParams) {
    return request<CustomerCreateResult>({
      url: '/admin/customer/create',
      method: 'post',
      data
    })
  },

  searchProjects(keyword?: string) {
    return request<ProjectOption[]>({
      url: '/admin/customer/project/search',
      method: 'get',
      params: { keyword: keyword || '' }
    })
  },

  searchAlliances(keyword?: string) {
    return request<AllianceOption[]>({
      url: '/admin/customer/alliance/search',
      method: 'get',
      params: { keyword: keyword || '' }
    })
  },

  customerVisits(customerId: number) {
    return request<CustomerVisitRecord[]>({
      url: '/admin/customer/visit/list',
      method: 'get',
      params: { customerId }
    })
  },

  customerDeals(customerId: number) {
    return request<CustomerDealRecord[]>({
      url: '/admin/customer/deal/list',
      method: 'get',
      params: { customerId }
    })
  },

  customerDealProbability(customerId: number) {
    return request<number>({
      url: '/admin/ai/analysis/probability',
      method: 'get',
      params: { customerId }
    }).catch(() => 0)
  },

  // ---------- 联盟商 ----------
  alliancePage(params: AllianceQueryParams) {
    return request<PageResult<AllianceRecord>>({
      url: '/admin/alliance/page',
      method: 'get',
      params: {
        page: params.page,
        pageSize: params.pageSize,
        companyName: params.companyName,
        status: params.status,
        level: params.level
      }
    })
  },

  allianceDetail(id: number) {
    return request<AllianceRecord>({
      url: `/admin/alliance/${id}`,
      method: 'get'
    })
  },

  allianceCreate(data: AllianceCreateParams) {
    return request<number>({
      url: '/admin/alliance/create',
      method: 'post',
      data: {
        companyName: data.companyName,
        officeAddress: data.officeAddress,
        contactName: data.contactName,
        contactPhone: data.contactPhone,
        staffSize: data.staffSize,
        level: data.level,
        channelUserId: data.channelUserId
      }
    })
  },

  allianceUpdate(data: AllianceCreateParams & { id?: number }) {
    return request<void>({
      url: '/admin/alliance/update',
      method: 'put',
      data
    })
  },

  allianceEnable(id: number) {
    return request<void>({
      url: `/admin/alliance/enable/${id}`,
      method: 'post'
    })
  },

  allianceDisable(id: number) {
    return request<void>({
      url: `/admin/alliance/disable/${id}`,
      method: 'post'
    })
  },

  allianceStats(id: number) {
    return request<AllianceStatsRecord>({
      url: `/admin/alliance/stats/${id}`,
      method: 'get'
    })
  },

  // ---------- 佣金 ----------
  commissionPage(params: CommissionQueryParams) {
    return request<PageResult<CommissionRecord>>({
      url: '/admin/commission/page',
      method: 'get',
      params: {
        page: params.page,
        pageSize: params.pageSize,
        projectId: params.projectId,
        allianceId: params.allianceId,
        auditStatus: params.auditStatus
      }
    })
  },

  commissionCreate(data: CommissionCreateParams) {
    return request<number>({
      url: '/admin/commission/create',
      method: 'post',
      data
    })
  },

  commissionAudit(data: CommissionAuditParams) {
    return request<void>({
      url: '/admin/commission/audit',
      method: 'post',
      data
    })
  },

  // ---------- 佣金多级审批流（业务员 → 主管 → 财务 → 放款） ----------
  commissionSubmit(data: CommissionSubmitParams) {
    return request<void>({
      url: '/admin/commission/submit',
      method: 'post',
      data
    })
  },

  commissionApproveByManager(data: CommissionApprovalActionParams) {
    return request<void>({
      url: '/admin/commission/approve/manager',
      method: 'post',
      data
    })
  },

  commissionApproveByFinance(data: CommissionApprovalActionParams) {
    return request<void>({
      url: '/admin/commission/approve/finance',
      method: 'post',
      data
    })
  },

  commissionMarkPaid(data: CommissionApprovalActionParams) {
    return request<void>({
      url: '/admin/commission/approve/payment',
      method: 'post',
      data
    })
  },

  commissionApprovalList(commissionId: number) {
    return request<CommissionApprovalRecord[]>({
      url: '/admin/commission/approval/list',
      method: 'get',
      params: { commissionId }
    }).catch(() => [] as CommissionApprovalRecord[])
  },

  commissionChangeLog(commissionId: number) {
    return request<CommissionChangeLogRecord[]>({
      url: '/admin/commission/changelog',
      method: 'get',
      params: { commissionId }
    })
  },

  // ---------- 考勤（对接 /admin/attendance） ----------
  attendanceRecords(params: { userId?: number; startDate?: string; endDate?: string }) {
    return request<AttendanceRecordItem[]>({
      url: '/admin/attendance/records',
      method: 'get',
      params: { userId: params.userId, startDate: params.startDate, endDate: params.endDate }
    }).catch(() => [])
  },

  attendanceMonthly(params: { userId: number; year: number; month: number }) {
    return request<AttendanceMonthlyRecord>({
      url: '/admin/attendance/monthly',
      method: 'get',
      params
    }).catch(() => ({
      userId: params.userId,
      year: params.year,
      month: params.month,
      attendanceDays: 0,
      lateTimes: 0,
      earlyLeaveTimes: 0,
      leaveDays: 0,
      overtimeHours: 0
    }))
  },

  leaveRequestList(params: { userId?: number; status?: number }) {
    return request<LeaveRequestItem[]>({
      url: '/admin/attendance/leave/list',
      method: 'get',
      params: { userId: params.userId, status: params.status }
    }).catch(() => [])
  },

  compensateRequestList(params: { userId?: number; status?: number }) {
    return request<CompensateRequestItem[]>({
      url: '/admin/attendance/compensate/list',
      method: 'get',
      params: { userId: params.userId, status: params.status }
    }).catch(() => [])
  },

  // ---------- 付款审批 ----------
  paymentPage(params: PaymentQueryParams) {
    return request<PageResult<PaymentRecord>>({
      url: '/admin/payment/page',
      method: 'get',
      params: {
        page: params.page,
        pageSize: params.pageSize,
        requestType: params.requestType,
        status: params.status,
        applicantId: params.applicantId
      }
    })
  },

  paymentCreate(data: PaymentCreateParams) {
    return request<number>({
      url: '/admin/payment/create',
      method: 'post',
      data
    })
  },

  paymentApprove(data: PaymentApproveParams) {
    return request<void>({
      url: '/admin/payment/approve',
      method: 'post',
      data: {
        requestId: data.requestId,
        approverId: data.approverId,
        approved: data.approved,
        remark: data.remark
      }
    })
  },

  paymentApprovals(requestId: number) {
    return request<PaymentApprovalRecord[]>({
      url: '/admin/payment/approvals',
      method: 'get',
      params: { requestId }
    })
  },

  // ---------- 项目 ----------
  projectPage(params: ProjectQueryParams) {
    return request<PageResult<ProjectRecord>>({
      url: '/admin/project/page',
      method: 'get',
      params: {
        page: params.page,
        pageSize: params.pageSize,
        projectName: params.projectName,
        district: params.district,
        projectType: params.projectType,
        status: params.status
      }
    })
  },

  projectDetail(id: number) {
    return request<ProjectRecord>({
      url: `/admin/project/${id}`,
      method: 'get'
    })
  },

  projectCreate(data: ProjectCreateParams) {
    return request<number>({
      url: '/admin/project/create',
      method: 'post',
      data
    })
  },

  projectUpdate(data: ProjectCreateParams & { id?: number }) {
    return request<void>({
      url: '/admin/project/update',
      method: 'put',
      data
    })
  },

  saveProjectCommissionRule(data: ProjectCommissionRuleCreateParams) {
    return request<number>({
      url: '/admin/project/commission-rule',
      method: 'post',
      data
    })
  },

  activeProjectCommissionRule(projectId: number) {
    return request<ProjectCommissionRuleRecord>({
      url: `/admin/project/commission-rule/active/${projectId}`,
      method: 'get'
    })
  },

  projectCommissionRuleVersions(projectId: number) {
    return request<ProjectCommissionRuleRecord[]>({
      url: `/admin/project/commission-rule/versions/${projectId}`,
      method: 'get'
    })
  },

  // ---------- 数据统计仪表盘 ----------
  dashboardOverview(params: { startDate?: string; endDate?: string }) {
    return request<DashboardOverviewRecord>({
      url: '/admin/dashboard/overview',
      method: 'get',
      params: { startDate: params.startDate, endDate: params.endDate }
    })
  },

  dashboardFunnel(params: { projectId?: number; allianceId?: number; startDate?: string; endDate?: string }) {
    return request<DashboardFunnelRecord>({
      url: '/admin/dashboard/funnel',
      method: 'get',
      params: {
        projectId: params.projectId,
        allianceId: params.allianceId,
        startDate: params.startDate,
        endDate: params.endDate
      }
    })
  },

  dashboardRanking(params: { startDate?: string; endDate?: string }) {
    return request<DashboardRankingRecord>({
      url: '/admin/dashboard/ranking',
      method: 'get',
      params: { startDate: params.startDate, endDate: params.endDate }
    })
  },

  // ---------- AI 对话（对接 /admin/ai/chat） ----------
  aiChat(message: string, conversationId?: string, projectId?: number) {
    return request<AiChatRecord & { conversationId?: string; routedAgent?: string; structuredData?: Record<string, unknown> }>({
      url: '/admin/ai/chat',
      method: 'post',
      data: { message, conversationId: conversationId || null, projectId: projectId || null }
    })
  },

  aiKnowledgeUpload(file: File, projectId?: number) {
    const formData = new FormData()
    formData.append('file', file)
    if (projectId != null && projectId > 0) {
      formData.append('projectId', String(projectId))
    }
    return request<number>({
      url: '/admin/ai/knowledge/upload',
      method: 'post',
      data: formData
    })
  },

  aiKnowledgeDocs() {
    return request<AiKnowledgeDocRecord[]>({
      url: '/admin/ai/knowledge/docs',
      method: 'get'
    }).catch(() => [])
  },

  aiKnowledgeDeleteDoc(id: number) {
    return request<void>({
      url: `/admin/ai/knowledge/docs/${id}`,
      method: 'delete'
    })
  },

  // ---------- 销售漏斗 / 商机 ----------
  pipelineStages() {
    return request<PipelineStageRecord[]>({
      url: '/admin/pipeline/stages',
      method: 'get'
    }).catch(() => [] as PipelineStageRecord[])
  },

  pipelineCreateOpportunity(data: OpportunityCreateParams) {
    return request<number>({
      url: '/admin/pipeline/opportunity',
      method: 'post',
      data
    })
  },

  pipelineMoveStage(data: OpportunityMoveStageParams) {
    return request<void>({
      url: '/admin/pipeline/opportunity/move',
      method: 'post',
      data
    })
  },

  pipelineByStage(stageId: number) {
    return request<OpportunityRecord[]>({
      url: '/admin/pipeline/opportunity/by-stage',
      method: 'get',
      params: { stageId }
    }).catch(() => [] as OpportunityRecord[])
  },

  pipelineStageLogs(opportunityId: number) {
    return request<OpportunityStageLogRecord[]>({
      url: `/admin/pipeline/opportunity/${opportunityId}/stage-logs`,
      method: 'get'
    }).catch(() => [] as OpportunityStageLogRecord[])
  },

  // ---------- 销售漏斗：任务文档命名别名 ----------
  opportunityCreate(data: OpportunityCreateParams) {
    return this.pipelineCreateOpportunity(data)
  },

  opportunityMoveStage(data: OpportunityMoveStageParams) {
    return this.pipelineMoveStage(data)
  },

  opportunityListByStage(stageId: number) {
    return this.pipelineByStage(stageId)
  },

  opportunityStageLogs(opportunityId: number) {
    return this.pipelineStageLogs(opportunityId)
  },

  // ---------- 报表文件下载（GET /admin/report/file/download，二进制流） ----------
  /**
   * 拼接报表下载 URL（浏览器原生跳转下载，自动带 Sa-Token）
   * @param type 报表类型 code：sales-performance / customer-analysis / commission-list / customer-followup-report
   * @param startDate yyyy-MM-dd
   * @param endDate yyyy-MM-dd
   * @param customerId 客户 ID（仅 customer-followup-report 等需要）
   *
   * 注意：当前 axios baseURL 为 '/api'，浏览器跳转无法走 axios 拦截器，
   *      因此 URL 含 /api 前缀；Sa-Token 通过 localStorage / cookie 在同域请求时由后端识别。
   *      若需要鉴权 header，应改用 fetchBlobUrl/downloadBlob 而非直接跳转。
   */
  reportFileDownloadUrl(
    type: ReportFileType,
    startDate?: string,
    endDate?: string,
    customerId?: number
  ): string {
    const params = new URLSearchParams()
    params.append('type', type)
    if (startDate) params.append('startDate', startDate)
    if (endDate) params.append('endDate', endDate)
    if (customerId != null) params.append('customerId', String(customerId))
    return `/api/admin/report/file/download?${params.toString()}`
  },

  /**
   * 带认证下载报表（推荐：经过 axios 拦截器，自动带 Authorization header）
   */
  reportFileDownload(
    type: ReportFileType,
    startDate?: string,
    endDate?: string,
    customerId?: number
  ) {
    return request<Blob>({
      url: '/admin/report/file/download',
      method: 'get',
      responseType: 'blob',
      params: { type, startDate, endDate, customerId }
    })
  },

  // ---------- 工单（TODO：后端 Controller 尚未实现，URL 按 PipelineController 风格猜测） ----------
  ticketCreate(data: TicketCreateParams) {
    return request<number>({
      url: '/admin/ticket',
      method: 'post',
      data
    })
  },

  ticketAssign(id: number, data: { assigneeId: number; content?: string }) {
    return request<void>({
      url: `/admin/ticket/${id}/assign`,
      method: 'post',
      data
    })
  },

  ticketStart(id: number) {
    return request<void>({
      url: `/admin/ticket/${id}/start`,
      method: 'post'
    })
  },

  ticketReply(id: number, data: { content: string }) {
    return request<void>({
      url: `/admin/ticket/${id}/reply`,
      method: 'post',
      data
    })
  },

  ticketResolve(id: number, data: { content: string }) {
    return request<void>({
      url: `/admin/ticket/${id}/resolve`,
      method: 'post',
      data
    })
  },

  ticketClose(id: number) {
    return request<void>({
      url: `/admin/ticket/${id}/close`,
      method: 'post'
    })
  },

  ticketCancel(id: number, data: { content: string }) {
    return request<void>({
      url: `/admin/ticket/${id}/cancel`,
      method: 'post',
      data
    })
  },

  ticketReopen(id: number, data: { content: string }) {
    return request<void>({
      url: `/admin/ticket/${id}/reopen`,
      method: 'post',
      data
    })
  },

  ticketMyOpen(userId?: number) {
    return request<TicketRecord[]>({
      url: '/admin/ticket/my-open',
      method: 'get',
      params: { userId }
    }).catch(() => [] as TicketRecord[])
  },

  ticketLogs(id: number) {
    return request<TicketLogRecord[]>({
      url: `/admin/ticket/${id}/logs`,
      method: 'get'
    }).catch(() => [] as TicketLogRecord[])
  }
}
