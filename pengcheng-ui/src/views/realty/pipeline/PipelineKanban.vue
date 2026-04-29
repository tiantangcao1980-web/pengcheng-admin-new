<template>
  <NSpace vertical :size="16">
    <NPageHeader title="销售漏斗" subtitle="按阶段查看商机分布与流转">
      <template #extra>
        <NButton tertiary @click="reload" :loading="loading">刷新</NButton>
      </template>
    </NPageHeader>

    <div class="kanban-board">
      <div
        v-for="stage in stages"
        :key="stage.id"
        class="kanban-col"
        :style="{ borderTopColor: stage.color || '#909399' }"
      >
        <div class="kanban-col-header">
          <span class="kanban-col-name">{{ stage.name }}</span>
          <NTag :color="{ color: stage.color || '#909399' }" size="small" round>
            {{ (oppByStage[stage.id] || []).length }}
          </NTag>
        </div>
        <div class="kanban-col-meta">
          <span>胜率 {{ stage.winRate }}%</span>
          <span v-if="stage.isTerminal === 1">· 终态</span>
        </div>

        <div class="kanban-cards">
          <NEmpty
            v-if="!loading && (oppByStage[stage.id] || []).length === 0"
            size="small"
            description="暂无商机"
          />
          <NCard
            v-for="opp in (oppByStage[stage.id] || [])"
            :key="opp.id"
            size="small"
            class="kanban-card"
            hoverable
            @click="onCardClick(opp)"
          >
            <div class="card-title">{{ opp.title || `商机 #${opp.id}` }}</div>
            <div class="card-meta">
              <span v-if="opp.expectedAmount" class="amount">
                ¥{{ formatAmount(opp.expectedAmount) }}
              </span>
              <span v-if="opp.expectedCloseDate" class="close-date">
                · 预期 {{ opp.expectedCloseDate }}
              </span>
            </div>
            <div v-if="opp.nextAction" class="card-action">
              下一步：{{ opp.nextAction }}
            </div>
            <div class="card-footer">
              <NSpace size="small">
                <NButton
                  v-for="next in nextStagesOf(stage)"
                  :key="next.id"
                  size="tiny"
                  @click.stop="onMove(opp, next)"
                >
                  → {{ next.name }}
                </NButton>
              </NSpace>
            </div>
          </NCard>
        </div>
      </div>
    </div>

    <!-- 流失原因输入 -->
    <NModal v-model:show="showLostModal" preset="dialog" title="填写流失原因" positive-text="确认流失" @positive-click="confirmLost">
      <NInput
        v-model:value="lostRemark"
        type="textarea"
        :rows="3"
        placeholder="请填写流失原因（必填）"
      />
    </NModal>
  </NSpace>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NEmpty,
  NInput,
  NModal,
  NPageHeader,
  NSpace,
  NTag,
  useMessage
} from 'naive-ui'
import {
  realtyApi,
  type OpportunityRecord,
  type PipelineStageRecord
} from '@/api/realty'

const message = useMessage()

const stages = ref<PipelineStageRecord[]>([])
const oppByStage = reactive<Record<number, OpportunityRecord[]>>({})
const loading = ref(false)

const showLostModal = ref(false)
const lostRemark = ref('')
const pendingMove = ref<{ opp: OpportunityRecord; toStage: PipelineStageRecord } | null>(null)

async function reload() {
  loading.value = true
  try {
    stages.value = await realtyApi.pipelineStages()
    // 并行加载每列商机
    await Promise.all(
      stages.value.map(async stage => {
        oppByStage[stage.id] = await realtyApi.pipelineByStage(stage.id)
      })
    )
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

/** 当前阶段的"下一步可流转"阶段：取 orderNo > 当前 + 流失（始终可达） */
function nextStagesOf(current: PipelineStageRecord): PipelineStageRecord[] {
  return stages.value.filter(s => {
    if (s.id === current.id) return false
    if (current.isTerminal === 1) return false  // 终态不可再流转
    if (s.code === 'LOST') return true          // 流失始终可选
    return s.orderNo === current.orderNo + 1    // 仅推进到相邻下一阶段
  })
}

async function onMove(opp: OpportunityRecord, toStage: PipelineStageRecord) {
  if (toStage.code === 'LOST') {
    pendingMove.value = { opp, toStage }
    lostRemark.value = ''
    showLostModal.value = true
    return
  }
  await doMove(opp, toStage, undefined)
}

async function confirmLost() {
  if (!pendingMove.value) return
  if (!lostRemark.value.trim()) {
    message.warning('请填写流失原因')
    return
  }
  await doMove(pendingMove.value.opp, pendingMove.value.toStage, lostRemark.value)
  pendingMove.value = null
}

async function doMove(opp: OpportunityRecord, toStage: PipelineStageRecord, remark?: string) {
  try {
    await realtyApi.pipelineMoveStage({
      opportunityId: opp.id,
      toStageId: toStage.id,
      remark
    })
    message.success(`已流转到 ${toStage.name}`)
    await reload()
  } catch (e: any) {
    message.error(e?.message || '流转失败')
  }
}

function onCardClick(_opp: OpportunityRecord) {
  // 留给 V1.0 收尾：打开详情抽屉显示阶段时间线（pipelineStageLogs）
}

function formatAmount(n?: number): string {
  if (!n) return '-'
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万`
  return n.toFixed(0)
}

const _allEmpty = computed(() =>
  stages.value.every(s => (oppByStage[s.id] || []).length === 0)
)

onMounted(reload)
</script>

<style scoped>
.kanban-board {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding: 8px 0;
  min-height: 480px;
}

.kanban-col {
  flex: 0 0 280px;
  background: var(--n-color-modal, #fafafa);
  border-radius: 6px;
  border-top: 3px solid #909399;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.kanban-col-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.kanban-col-name {
  font-weight: 600;
  font-size: 14px;
}

.kanban-col-meta {
  font-size: 12px;
  color: var(--n-text-color-3, #999);
  margin-bottom: 12px;
}

.kanban-cards {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  max-height: 600px;
}

.kanban-card {
  cursor: pointer;
}

.card-title {
  font-weight: 500;
  margin-bottom: 6px;
}

.card-meta {
  font-size: 12px;
  color: var(--n-text-color-2, #666);
  margin-bottom: 4px;
}

.amount {
  color: #67c23a;
  font-weight: 500;
}

.card-action {
  font-size: 12px;
  color: var(--n-text-color-3, #999);
  margin-top: 4px;
  margin-bottom: 8px;
}

.card-footer {
  border-top: 1px dashed var(--n-divider-color, #eee);
  padding-top: 6px;
}
</style>
