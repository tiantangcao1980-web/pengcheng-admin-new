<template>
  <NDrawer
    :show="show"
    :width="540"
    placement="right"
    @update:show="(v: boolean) => emit('update:show', v)"
  >
    <NDrawerContent title="佣金审批流" closable>
      <!-- 流程进度 -->
      <NSteps
        v-if="currentNode !== 'REJECTED'"
        :current="stepIndex"
        :status="stepStatus"
        size="small"
      >
        <NStep title="提交" />
        <NStep title="主管审批" />
        <NStep title="财务审批" />
        <NStep title="放款" />
      </NSteps>
      <NAlert v-else type="error" :show-icon="true" style="margin-bottom: 16px">
        审批已驳回，业务员可重新提交
      </NAlert>

      <NDivider />

      <!-- 当前节点显示 -->
      <NSpace vertical :size="8">
        <div>
          <span class="label">当前节点：</span>
          <NTag :type="nodeTagType" size="medium">{{ currentNodeLabel }}</NTag>
        </div>
      </NSpace>

      <NDivider>审批历史</NDivider>

      <!-- 审批历史时间线 -->
      <NTimeline v-if="approvalRecords.length > 0">
        <NTimelineItem
          v-for="record in approvalRecords"
          :key="record.id"
          :type="record.result === 1 ? 'success' : 'error'"
          :title="`${nodeName(record.node)}・${record.result === 1 ? '通过' : '驳回'}`"
          :content="record.remark || '无备注'"
          :time="record.approvalTime"
        />
      </NTimeline>
      <NEmpty v-else description="暂无审批记录" />

      <NDivider>操作</NDivider>

      <!-- 操作区 -->
      <div v-if="canSubmit">
        <NButton type="primary" block :loading="loading" @click="onSubmit">
          提交审批
        </NButton>
      </div>
      <div v-else-if="canApprove">
        <NSpace vertical :size="12">
          <NInput
            v-model:value="remark"
            type="textarea"
            placeholder="审批备注（驳回必填）"
            :rows="3"
          />
          <NSpace>
            <NButton type="primary" :loading="loading" @click="onApprove(true)">
              {{ approveButtonLabel }}
            </NButton>
            <NButton type="error" :loading="loading" @click="onApprove(false)">
              驳回
            </NButton>
          </NSpace>
        </NSpace>
      </div>
      <NEmpty v-else description="当前节点无可执行操作" />
    </NDrawerContent>
  </NDrawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  NAlert,
  NButton,
  NDivider,
  NDrawer,
  NDrawerContent,
  NEmpty,
  NInput,
  NSpace,
  NStep,
  NSteps,
  NTag,
  NTimeline,
  NTimelineItem,
  useMessage
} from 'naive-ui'
import {
  realtyApi,
  COMMISSION_NODE_LABEL,
  type CommissionApprovalNode,
  type CommissionApprovalRecord
} from '@/api/realty'

const props = defineProps<{
  show: boolean
  commissionId: number | null
  currentNode: CommissionApprovalNode | null
  /** 当前用户角色，决定显示哪些操作按钮 */
  userRole: 'submitter' | 'manager' | 'finance' | 'payment' | 'viewer'
  currentUserId?: number
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'changed'): void
}>()

const message = useMessage()
const loading = ref(false)
const remark = ref('')
const approvalRecords = ref<CommissionApprovalRecord[]>([])

const currentNodeLabel = computed(() =>
  props.currentNode ? COMMISSION_NODE_LABEL[props.currentNode] : '草稿'
)

const stepIndex = computed(() => {
  switch (props.currentNode) {
    case null:
    case 'DRAFT':
      return 0
    case 'SUBMITTED':
      return 1
    case 'MANAGER_APPROVED':
      return 2
    case 'FINANCE_APPROVED':
      return 3
    case 'PAID':
      return 4
    default:
      return 0
  }
})

const stepStatus = computed<'process' | 'finish' | 'error'>(() =>
  props.currentNode === 'PAID' ? 'finish' : 'process'
)

const nodeTagType = computed<'default' | 'success' | 'warning' | 'error'>(() => {
  if (props.currentNode === 'PAID') return 'success'
  if (props.currentNode === 'REJECTED') return 'error'
  if (props.currentNode == null || props.currentNode === 'DRAFT') return 'default'
  return 'warning'
})

const canSubmit = computed(
  () =>
    props.userRole === 'submitter' &&
    (props.currentNode == null ||
      props.currentNode === 'DRAFT' ||
      props.currentNode === 'REJECTED')
)

const canApprove = computed(() => {
  if (props.userRole === 'manager' && props.currentNode === 'SUBMITTED') return true
  if (props.userRole === 'finance' && props.currentNode === 'MANAGER_APPROVED') return true
  if (props.userRole === 'payment' && props.currentNode === 'FINANCE_APPROVED') return true
  return false
})

const approveButtonLabel = computed(() => {
  if (props.userRole === 'payment') return '标记放款'
  return '通过'
})

function nodeName(node: string): string {
  if (node === 'MANAGER') return '主管审批'
  if (node === 'FINANCE') return '财务审批'
  if (node === 'PAYMENT') return '放款'
  return node
}

async function loadApprovalRecords() {
  if (!props.commissionId) {
    approvalRecords.value = []
    return
  }
  approvalRecords.value = await realtyApi.commissionApprovalList(props.commissionId)
}

async function onSubmit() {
  if (!props.commissionId) return
  loading.value = true
  try {
    await realtyApi.commissionSubmit({
      commissionId: props.commissionId,
      submitterId: props.currentUserId,
      remark: remark.value || undefined
    })
    message.success('已提交审批')
    remark.value = ''
    emit('changed')
    emit('update:show', false)
  } catch (e: any) {
    message.error(e?.message || '提交失败')
  } finally {
    loading.value = false
  }
}

async function onApprove(approved: boolean) {
  if (!props.commissionId) return
  if (!approved && !remark.value.trim()) {
    message.warning('驳回必须填写备注')
    return
  }
  loading.value = true
  try {
    const params = {
      commissionId: props.commissionId,
      approverId: props.currentUserId,
      approved,
      remark: remark.value || undefined
    }
    if (props.userRole === 'manager') {
      await realtyApi.commissionApproveByManager(params)
    } else if (props.userRole === 'finance') {
      await realtyApi.commissionApproveByFinance(params)
    } else if (props.userRole === 'payment') {
      await realtyApi.commissionMarkPaid(params)
    }
    message.success(approved ? '审批通过' : '已驳回')
    remark.value = ''
    emit('changed')
    emit('update:show', false)
  } catch (e: any) {
    message.error(e?.message || '审批失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.show, props.commissionId],
  ([show]) => {
    if (show) {
      remark.value = ''
      loadApprovalRecords()
    }
  }
)
</script>

<style scoped>
.label {
  color: var(--n-text-color-3, #666);
  margin-right: 8px;
}
</style>
