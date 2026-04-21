<template>
  <div class="review-360-page">
    <n-grid :cols="24" :x-gap="16">
      <!-- 左侧：评估任务列表 -->
      <n-gi :span="16">
        <n-card title="我的评估任务">
          <template #header-extra>
            <n-space>
              <n-button size="small" @click="loadPendingReviews">刷新</n-button>
            </n-space>
          </template>

          <n-data-table
            :columns="taskColumns"
            :data="pendingReviews"
            :loading="taskLoading"
            :pagination="taskPagination"
          />
        </n-card>

        <n-card title="评估结果查询" style="margin-top: 16px">
          <n-form inline :model="resultFilter">
            <n-form-item label="考核周期">
              <n-select
                v-model:value="resultFilter.periodId"
                :options="periodOptions"
                style="width: 200px"
              />
            </n-form-item>
            <n-form-item label="被评估人">
              <n-select
                v-model:value="resultFilter.userId"
                :options="userOptions"
                label-field="nickname"
                value-field="id"
                style="width: 200px"
                filterable
              />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" @click="loadReviewResult">查询</n-button>
            </n-form-item>
          </n-form>

          <div v-if="reviewResult" class="result-card">
            <div class="result-header">
              <h3>{{ reviewResult.userName }} - 360 度评估结果</h3>
              <n-tag :type="gradeType(reviewResult.grade)" size="large">{{ reviewResult.grade }}级</n-tag>
            </div>

            <n-grid :cols="4" :x-gap="12" :y-gap="12" class="score-grid">
              <n-gi>
                <n-statistic label="自评分数" :value="reviewResult.selfScore" />
              </n-gi>
              <n-gi>
                <n-statistic label="上级评分" :value="reviewResult.managerScore" />
              </n-gi>
              <n-gi>
                <n-statistic label="同事评分" :value="reviewResult.peerScore" />
              </n-gi>
              <n-gi>
                <n-statistic label="下级评分" :value="reviewResult.subordinateScore" />
              </n-gi>
            </n-grid>

            <n-divider>综合得分</n-divider>
            <div class="final-score">
              <n-progress
                type="dashboard"
                :percentage="reviewResult.finalScore"
                :status="reviewResult.finalScore >= 90 ? 'success' : reviewResult.finalScore >= 80 ? 'success' : 'warning'"
              />
            </div>

            <n-divider>评价详情</n-divider>
            <n-timeline>
              <n-timeline-item
                v-for="review in reviewResult.reviews"
                :key="review.id"
                :type="reviewTypeColor(review.reviewType)"
                :title="review.reviewTypeName"
                :content="`${review.reviewerName}：${review.score}分`"
              >
                <template #footer>
                  <div class="review-detail">
                    <p v-if="review.comment"><strong>评价：</strong>{{ review.comment }}</p>
                    <p v-if="review.strengths"><strong>优点：</strong>{{ review.strengths }}</p>
                    <p v-if="review.improvements"><strong>待改进：</strong>{{ review.improvements }}</p>
                  </div>
                </template>
              </n-timeline-item>
            </n-timeline>
          </div>
        </n-card>
      </n-gi>

      <!-- 右侧：评估配置与统计 -->
      <n-gi :span="8">
        <n-card title="权重配置">
          <n-form label-placement="left" label-width="80">
            <n-form-item label="自评权重">
              <n-input-number v-model:value="weightConfig.selfWeight" :min="0" :max="1" :step="0.05" style="width: 100%">
                <template #suffix>{{ (weightConfig.selfWeight * 100).toFixed(0) }}%</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="上级权重">
              <n-input-number v-model:value="weightConfig.managerWeight" :min="0" :max="1" :step="0.05" style="width: 100%">
                <template #suffix>{{ (weightConfig.managerWeight * 100).toFixed(0) }}%</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="同事权重">
              <n-input-number v-model:value="weightConfig.peerWeight" :min="0" :max="1" :step="0.05" style="width: 100%">
                <template #suffix>{{ (weightConfig.peerWeight * 100).toFixed(0) }}%</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="下级权重">
              <n-input-number v-model:value="weightConfig.subordinateWeight" :min="0" :max="1" :step="0.05" style="width: 100%">
                <template #suffix>{{ (weightConfig.subordinateWeight * 100).toFixed(0) }}%</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="最小人数">
              <n-input-number v-model:value="weightConfig.minReviewers" :min="1" :max="20" style="width: 100%" />
            </n-form-item>
            <n-form-item label="匿名评估">
              <n-switch v-model:value="weightConfig.anonymous" />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" :loading="weightSaving" @click="saveWeightConfig">保存配置</n-button>
            </n-form-item>
          </n-form>
        </n-card>

        <n-card title="评估统计" style="margin-top: 16px" v-if="reviewResult?.stats">
          <n-space vertical>
            <n-statistic label="总评估人数" :value="reviewResult.stats.totalCount" />
            <n-statistic label="自评" :value="reviewResult.stats.selfCount" />
            <n-statistic label="上级评价" :value="reviewResult.stats.managerCount" />
            <n-statistic label="同事评价" :value="reviewResult.stats.peerCount" />
            <n-statistic label="下级评价" :value="reviewResult.stats.subordinateCount" />
          </n-space>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 评估表单弹窗 -->
    <n-modal v-model:show="showReviewForm" preset="card" :title="reviewFormTitle" style="width: 600px">
      <n-form :model="reviewForm" label-placement="left" label-width="100">
        <n-form-item label="被评估人">
          <span>{{ currentRevieweeName }}</span>
        </n-form-item>
        <n-form-item label="评估类型">
          <span>{{ reviewTypeName }}</span>
        </n-form-item>
        <n-form-item label="总分" required>
          <n-input-number v-model:value="reviewForm.totalScore" :min="0" :max="100" style="width: 100%" placeholder="0-100 分" />
        </n-form-item>
        <n-form-item label="评价意见">
          <n-input v-model:value="reviewForm.comment" type="textarea" placeholder="请写下您的评价" :rows="3" />
        </n-form-item>
        <n-form-item label="优点/长处">
          <n-input v-model:value="reviewForm.strengths" type="textarea" placeholder="被评估人的优点和长处" :rows="2" />
        </n-form-item>
        <n-form-item label="待改进项">
          <n-input v-model:value="reviewForm.improvements" type="textarea" placeholder="需要改进的方面" :rows="2" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showReviewForm = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submitReview">提交评估</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { NButton, NTag, NProgress, NTimeline, NTimelineItem, NStatistic, useMessage, type DataTableColumns } from 'naive-ui'
import { request } from '@/utils/request'
import { userApi } from '@/api/system'

interface PendingReview {
  periodId: number
  userId: number
  reviewType: number
}

interface ReviewResult {
  userName: string
  selfScore: number
  managerScore: number
  peerScore: number
  subordinateScore: number
  finalScore: number
  grade: string
  stats: {
    selfCount: number
    managerCount: number
    peerCount: number
    subordinateCount: number
    totalCount: number
  }
  reviews: Array<{
    reviewType: number
    reviewTypeName: string
    reviewerName: string
    score: number
    comment: string
    strengths: string
    improvements: string
  }>
}

interface WeightConfig {
  selfWeight: number
  managerWeight: number
  peerWeight: number
  subordinateWeight: number
  minReviewers: number
  anonymous: boolean
}

const message = useMessage()

const pendingReviews = ref<PendingReview[]>([])
const taskLoading = ref(false)
const taskPagination = reactive({ page: 1, pageSize: 10, itemCount: 0 })

const periodOptions = ref<any[]>([])
const userOptions = ref<any[]>([])
const resultFilter = reactive({ periodId: null as number | null, userId: null as number | null })
const reviewResult = ref<ReviewResult | null>(null)

const weightConfig = ref<WeightConfig>({
  selfWeight: 0.1,
  managerWeight: 0.4,
  peerWeight: 0.3,
  subordinateWeight: 0.2,
  minReviewers: 3,
  anonymous: true
})
const weightSaving = ref(false)

const showReviewForm = ref(false)
const submitting = ref(false)
const reviewForm = reactive({
  periodId: null as number | null,
  userId: null as number | null,
  reviewType: 1,
  totalScore: null as number | null,
  comment: '',
  strengths: '',
  improvements: ''
})
const currentRevieweeName = ref('')
const reviewTypeName = ref('')

const taskColumns: DataTableColumns<PendingReview> = [
  { title: '考核周期', key: 'periodId', width: 120, render: (row) => `周期${row.periodId}` },
  { title: '被评估人', key: 'userId', width: 120, render: (row) => `用户${row.userId}` },
  {
    title: '评估类型', key: 'reviewType', width: 100,
    render: (row) => h(NTag, {
      size: 'small',
      type: row.reviewType === 1 ? 'info' : row.reviewType === 2 ? 'success' : row.reviewType === 3 ? 'warning' : 'error'
    }, { default: () => getReviewTypeName(row.reviewType) })
  },
  {
    title: '操作', key: 'action', width: 100,
    render: (row) => h(NButton, {
      size: 'small',
      type: 'primary',
      onClick: () => openReviewForm(row)
    }, { default: () => '评估' })
  }
]

function getReviewTypeName(type: number): string {
  const map: Record<number, string> = { 1: '自评', 2: '上级', 3: '同事', 4: '下级' }
  return map[type] || '未知'
}

function gradeType(grade: string): 'success' | 'warning' | 'error' | 'default' {
  if (grade === 'A') return 'success'
  if (grade === 'B') return 'success'
  if (grade === 'C') return 'warning'
  return 'error'
}

function reviewTypeColor(type: number): 'default' | 'info' | 'success' | 'warning' | 'error' {
  const map: Record<number, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
    1: 'info', 2: 'success', 3: 'warning', 4: 'error'
  }
  return map[type] || 'default'
}

async function loadPendingReviews() {
  taskLoading.value = true
  try {
    const res: any = await request({ url: '/admin/hr/kpi/review/360/pending', method: 'get' })
    const data = res?.data || res || {}
    pendingReviews.value = data?.list || []
    taskPagination.itemCount = data?.total || 0
  } catch (err: any) {
    message.error(err?.message || '加载失败')
  } finally {
    taskLoading.value = false
  }
}

async function loadPeriods() {
  try {
    const res: any = await request({ url: '/admin/hr/kpi/periods', method: 'get', params: { pageNum: 1, pageSize: 50 } })
    const list = res?.data?.records || res?.records || []
    periodOptions.value = list.map((p: any) => ({ label: p.name, value: p.id }))
    if (periodOptions.value.length) resultFilter.periodId = periodOptions.value[0].value
  } catch {}
}

async function loadUserOptions() {
  try {
    const res: any = await userApi.page({ page: 1, pageSize: 100 })
    userOptions.value = res?.list || res?.data?.list || []
  } catch {}
}

async function loadReviewResult() {
  if (!resultFilter.periodId || !resultFilter.userId) {
    message.warning('请选择周期和被评估人')
    return
  }
  try {
    const res: any = await request({
      url: '/admin/hr/kpi/review/360/result',
      method: 'get',
      params: { periodId: resultFilter.periodId, userId: resultFilter.userId }
    })
    reviewResult.value = res?.data || res || null
  } catch (err: any) {
    message.error(err?.message || '查询失败')
    reviewResult.value = null
  }
}

async function loadWeightConfig() {
  try {
    const res: any = await request({ url: '/admin/hr/kpi/review/360/config', method: 'get' })
    const config = res?.data || res || {}
    weightConfig.value = {
      selfWeight: config.selfWeight ?? 0.1,
      managerWeight: config.managerWeight ?? 0.4,
      peerWeight: config.peerWeight ?? 0.3,
      subordinateWeight: config.subordinateWeight ?? 0.2,
      minReviewers: config.minReviewers ?? 3,
      anonymous: config.anonymous !== false
    }
  } catch {}
}

async function saveWeightConfig() {
  weightSaving.value = true
  try {
    await request({ url: '/admin/hr/kpi/review/360/config', method: 'post', data: weightConfig.value })
    message.success('配置已保存')
  } catch (err: any) {
    message.error(err?.message || '保存失败')
  } finally {
    weightSaving.value = false
  }
}

function openReviewForm(row: PendingReview) {
  reviewForm.periodId = row.periodId
  reviewForm.userId = row.userId
  reviewForm.reviewType = row.reviewType
  reviewForm.totalScore = null
  reviewForm.comment = ''
  reviewForm.strengths = ''
  reviewForm.improvements = ''
  currentRevieweeName.value = `用户${row.userId}`
  reviewTypeName.value = getReviewTypeName(row.reviewType)
  showReviewForm.value = true
}

async function submitReview() {
  if (!reviewForm.totalScore) {
    message.warning('请输入总分')
    return
  }
  submitting.value = true
  try {
    await request({ url: '/admin/hr/kpi/review/360', method: 'post', data: reviewForm })
    message.success('评估已提交')
    showReviewForm.value = false
    loadPendingReviews()
  } catch (err: any) {
    message.error(err?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPendingReviews()
  loadPeriods()
  loadUserOptions()
  loadWeightConfig()
})
</script>

<style scoped>
.review-360-page { padding: 20px; }
.result-card { padding: 16px 0; }
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.result-header h3 { margin: 0; font-size: 18px; }
.score-grid { margin-bottom: 16px; }
.final-score { display: flex; justify-content: center; padding: 20px 0; }
.review-detail { background: #f5f5f5; padding: 12px; border-radius: 6px; margin-top: 8px; }
.review-detail p { margin: 4px 0; font-size: 13px; }
</style>
