<template>
  <div class="okr-page">
    <!-- 顶部周期切换器 -->
    <div class="okr-header">
      <div class="period-selector">
        <span class="label">OKR 周期：</span>
        <el-select
          v-model="selectedPeriodId"
          placeholder="选择周期"
          style="width: 180px"
          @change="onPeriodChange"
        >
          <el-option
            v-for="p in periods"
            :key="p.id"
            :label="p.name"
            :value="p.id"
          />
        </el-select>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openCreateObjectiveDialog(null)">
          新建目标
        </el-button>
      </div>
    </div>

    <!-- 主体：左侧目标树 + 右侧详情 -->
    <div class="okr-body">
      <!-- 左侧目标对齐树 -->
      <div class="objective-tree-panel">
        <div class="panel-title">目标列表</div>
        <el-tree
          :data="objectiveTree"
          :props="treeProps"
          node-key="id"
          highlight-current
          :expand-on-click-node="false"
          @node-click="selectObjective"
        >
          <template #default="{ node, data }">
            <div class="tree-node">
              <span class="node-title">{{ data.title }}</span>
              <el-progress
                :percentage="data.progress || 0"
                :stroke-width="6"
                style="width: 80px"
              />
            </div>
          </template>
        </el-tree>
      </div>

      <!-- 右侧选中目标详情 -->
      <div class="objective-detail-panel" v-if="currentObjective">
        <!-- 目标信息 -->
        <div class="objective-info-card">
          <div class="objective-title">
            <span>{{ currentObjective.title }}</span>
            <el-tag :type="statusTagType(currentObjective.status)" size="small" style="margin-left: 8px">
              {{ statusLabel(currentObjective.status) }}
            </el-tag>
          </div>
          <p class="objective-desc" v-if="currentObjective.description">
            {{ currentObjective.description }}
          </p>
          <el-progress
            :percentage="currentObjective.progress || 0"
            :color="progressColor(currentObjective.progress)"
            style="margin-top: 8px"
          />
          <div class="objective-meta">
            <span>ownerType: {{ currentObjective.ownerType }}</span>
            <span style="margin-left: 16px">weight: {{ currentObjective.weight }}</span>
          </div>
          <div class="objective-actions" style="margin-top: 8px">
            <el-button size="small" @click="openCreateObjectiveDialog(currentObjective.id)">
              添加子目标
            </el-button>
            <el-button size="small" type="primary" @click="openCheckinDialog">
              提交 Check-in
            </el-button>
            <el-button size="small" @click="openSuggestKrDialog">
              AI 建议 KR
            </el-button>
          </div>
        </div>

        <!-- KR 列表 -->
        <div class="kr-section">
          <div class="section-title">
            关键结果（Key Results）
            <el-button size="small" type="primary" link @click="openCreateKrDialog">
              + 添加 KR
            </el-button>
          </div>
          <div v-if="keyResults.length === 0" class="empty-tip">暂无关键结果</div>
          <div
            v-for="kr in keyResults"
            :key="kr.id"
            class="kr-item"
          >
            <div class="kr-title">{{ kr.title }}</div>
            <div class="kr-meta">
              <span>{{ kr.measureType }}</span>
              <span v-if="kr.targetValue != null">目标值: {{ kr.targetValue }}{{ kr.unit || '' }}</span>
              <span>当前: {{ kr.currentValue || 0 }}</span>
            </div>
            <el-progress :percentage="kr.progress || 0" :stroke-width="8" style="margin-top: 4px" />
            <div class="kr-actions">
              <el-button size="small" link @click="openUpdateKrProgressDialog(kr)">更新进度</el-button>
            </div>
          </div>
        </div>

        <!-- Check-in 列表 -->
        <div class="checkin-section">
          <div class="section-title">Check-in 记录</div>
          <div v-if="checkins.length === 0" class="empty-tip">暂无 Check-in</div>
          <el-timeline>
            <el-timeline-item
              v-for="c in checkins"
              :key="c.id"
              :timestamp="c.createTime"
              placement="top"
            >
              <el-card shadow="never" size="small">
                <div>
                  <strong>第 {{ c.weekIndex }} 周</strong>
                  进度: {{ c.progress }}%
                  信心: {{ c.confidence }}/10
                </div>
                <p v-if="c.summary" style="margin: 4px 0 0">{{ c.summary }}</p>
                <p v-if="c.issues" style="color: #f56c6c; margin: 2px 0 0">阻碍: {{ c.issues }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>

      <div class="objective-detail-panel no-selection" v-else>
        <el-empty description="请在左侧选择一个目标" />
      </div>
    </div>

    <!-- 创建目标弹窗 -->
    <el-dialog v-model="createObjectiveVisible" title="新建目标" width="520px">
      <el-form :model="createObjectiveForm" label-width="90px">
        <el-form-item label="目标标题" required>
          <el-input v-model="createObjectiveForm.title" placeholder="请输入目标标题" />
        </el-form-item>
        <el-form-item label="目标描述">
          <el-input v-model="createObjectiveForm.description" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item label="负责人ID">
          <el-input v-model.number="createObjectiveForm.ownerId" />
        </el-form-item>
        <el-form-item label="负责人类型">
          <el-select v-model="createObjectiveForm.ownerType" style="width: 100%">
            <el-option label="用户" value="USER" />
            <el-option label="部门" value="DEPT" />
            <el-option label="公司" value="COMPANY" />
          </el-select>
        </el-form-item>
        <el-form-item label="权重">
          <el-input-number v-model="createObjectiveForm.weight" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="上级目标" v-if="createObjectiveParentId != null">
          <el-text>ID: {{ createObjectiveParentId }}</el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createObjectiveVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateObjective">确定</el-button>
      </template>
    </el-dialog>

    <!-- 创建 KR 弹窗 -->
    <el-dialog v-model="createKrVisible" title="新建关键结果" width="520px">
      <el-form :model="createKrForm" label-width="90px">
        <el-form-item label="KR 标题" required>
          <el-input v-model="createKrForm.title" placeholder="请输入关键结果" />
        </el-form-item>
        <el-form-item label="度量类型">
          <el-select v-model="createKrForm.measureType" style="width: 100%">
            <el-option label="数值 NUMBER" value="NUMBER" />
            <el-option label="百分比 PERCENT" value="PERCENT" />
            <el-option label="里程碑 MILESTONE" value="MILESTONE" />
            <el-option label="是/否 BOOLEAN" value="BOOLEAN" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标值">
          <el-input-number v-model="createKrForm.targetValue" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="createKrForm.unit" placeholder="如：个、%、万元" />
        </el-form-item>
        <el-form-item label="权重">
          <el-input-number v-model="createKrForm.weight" :min="1" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createKrVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateKr">确定</el-button>
      </template>
    </el-dialog>

    <!-- 更新 KR 进度弹窗 -->
    <el-dialog v-model="updateProgressVisible" title="更新 KR 进度" width="400px">
      <el-form :model="updateProgressForm" label-width="90px">
        <el-form-item label="当前值">
          <el-input-number v-model="updateProgressForm.currentValue" style="width: 100%" />
        </el-form-item>
        <el-form-item label="手动进度">
          <el-slider v-model="updateProgressForm.progress" :min="0" :max="100" show-input />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="updateProgressVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUpdateProgress">确定</el-button>
      </template>
    </el-dialog>

    <!-- Check-in 弹窗 -->
    <el-dialog v-model="checkinVisible" title="提交 Check-in" width="520px">
      <el-form :model="checkinForm" label-width="90px">
        <el-form-item label="本周周次">
          <el-input-number v-model="checkinForm.weekIndex" :min="1" :max="52" />
        </el-form-item>
        <el-form-item label="当前进度">
          <el-slider v-model="checkinForm.progress" :min="0" :max="100" show-input />
        </el-form-item>
        <el-form-item label="信心指数">
          <el-slider v-model="checkinForm.confidence" :min="1" :max="10" show-input />
        </el-form-item>
        <el-form-item label="本周总结">
          <el-input v-model="checkinForm.summary" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="阻碍">
          <el-input v-model="checkinForm.issues" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="下步计划">
          <el-input v-model="checkinForm.nextSteps" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkinVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCheckin">确定</el-button>
      </template>
    </el-dialog>

    <!-- AI 建议 KR 弹窗 -->
    <el-dialog v-model="suggestKrVisible" title="AI 建议关键结果" width="480px">
      <div v-if="suggestLoading" style="text-align: center; padding: 24px">
        <el-icon class="is-loading"><Loading /></el-icon>
        <p style="margin-top: 8px; color: #909399">AI 正在生成建议…</p>
      </div>
      <div v-else>
        <div v-if="suggestedKrs.length === 0" class="empty-tip">AI 暂时无法生成建议</div>
        <div v-for="(suggestion, idx) in suggestedKrs" :key="idx" class="suggestion-item">
          <el-checkbox v-model="selectedSuggestions[idx]" />
          <span style="margin-left: 8px">{{ suggestion }}</span>
        </div>
        <div style="margin-top: 12px; color: #909399; font-size: 12px">
          勾选需要的建议，点击「采用」将自动创建为 KR（degree量类型默认 MILESTONE）
        </div>
      </div>
      <template #footer>
        <el-button @click="suggestKrVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="suggestLoading" @click="adoptSuggestedKrs">采用勾选</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import {
  okrPeriodApi,
  okrObjectiveApi,
  okrKeyResultApi,
  okrCheckinApi,
  type OkrPeriod,
  type OkrObjective,
  type OkrKeyResult,
  type OkrCheckin,
} from '@/api/okr'

// ==================== 周期 ====================
const periods = ref<OkrPeriod[]>([])
const selectedPeriodId = ref<number | undefined>()

async function loadPeriods() {
  const res = await okrPeriodApi.listAll()
  periods.value = (res as any).data || res || []
  if (periods.value.length && !selectedPeriodId.value) {
    const active = periods.value.find((p) => p.status === 1)
    selectedPeriodId.value = active ? active.id : periods.value[0].id
    await loadObjectiveTree()
  }
}

function onPeriodChange() {
  currentObjective.value = null
  loadObjectiveTree()
}

// ==================== 目标树 ====================
const objectiveTree = ref<any[]>([])
const treeProps = { label: 'title', children: 'children' }
const currentObjective = ref<OkrObjective | null>(null)

async function loadObjectiveTree() {
  if (!selectedPeriodId.value) return
  // 查顶级目标
  const topRes = await okrObjectiveApi.listTree({ periodId: selectedPeriodId.value, parentId: undefined })
  const tops: OkrObjective[] = (topRes as any).data || topRes || []
  // 为每个顶级目标加载子节点（简单一层递归）
  objectiveTree.value = await Promise.all(
    tops.map(async (obj) => {
      const childRes = await okrObjectiveApi.listTree({ periodId: selectedPeriodId.value, parentId: obj.id })
      const children: OkrObjective[] = (childRes as any).data || childRes || []
      return { ...obj, children }
    })
  )
}

async function selectObjective(obj: OkrObjective) {
  currentObjective.value = obj
  await Promise.all([loadKeyResults(), loadCheckins()])
}

// ==================== KR ====================
const keyResults = ref<OkrKeyResult[]>([])
const createKrVisible = ref(false)
const createKrForm = reactive<Partial<OkrKeyResult>>({
  measureType: 'NUMBER',
  weight: 25,
  targetValue: undefined,
})

async function loadKeyResults() {
  if (!currentObjective.value?.id) return
  const res = await okrKeyResultApi.list(currentObjective.value.id)
  keyResults.value = (res as any).data || res || []
}

function openCreateKrDialog() {
  Object.assign(createKrForm, { title: '', measureType: 'NUMBER', targetValue: undefined, unit: '', weight: 25 })
  createKrVisible.value = true
}

async function submitCreateKr() {
  if (!createKrForm.title) {
    ElMessage.warning('请填写 KR 标题')
    return
  }
  await okrKeyResultApi.create({ ...createKrForm, objectiveId: currentObjective.value!.id! } as OkrKeyResult)
  ElMessage.success('KR 创建成功')
  createKrVisible.value = false
  await loadKeyResults()
}

// 更新 KR 进度
const updateProgressVisible = ref(false)
const updateProgressForm = reactive({ keyResultId: 0, currentValue: 0, progress: 0 })

function openUpdateKrProgressDialog(kr: OkrKeyResult) {
  updateProgressForm.keyResultId = kr.id!
  updateProgressForm.currentValue = kr.currentValue || 0
  updateProgressForm.progress = kr.progress || 0
  updateProgressVisible.value = true
}

async function submitUpdateProgress() {
  await okrKeyResultApi.updateProgress({
    keyResultId: updateProgressForm.keyResultId,
    currentValue: updateProgressForm.currentValue,
    progress: updateProgressForm.progress,
  })
  ElMessage.success('进度更新成功')
  updateProgressVisible.value = false
  await loadKeyResults()
  // 刷新目标进度
  await loadObjectiveTree()
}

// ==================== Check-in ====================
const checkins = ref<OkrCheckin[]>([])
const checkinVisible = ref(false)
const checkinForm = reactive({
  weekIndex: 1,
  progress: 0,
  confidence: 5,
  summary: '',
  issues: '',
  nextSteps: '',
})

async function loadCheckins() {
  if (!currentObjective.value?.id) return
  const res = await okrCheckinApi.listByObjective(currentObjective.value.id)
  checkins.value = (res as any).data || res || []
}

function openCheckinDialog() {
  const now = new Date()
  const start = new Date(now.getFullYear(), 0, 1)
  const weekIndex = Math.ceil(((now.getTime() - start.getTime()) / 86400000 + start.getDay() + 1) / 7)
  Object.assign(checkinForm, {
    weekIndex,
    progress: currentObjective.value?.progress || 0,
    confidence: 5,
    summary: '',
    issues: '',
    nextSteps: '',
  })
  checkinVisible.value = true
}

async function submitCheckin() {
  await okrCheckinApi.submit({
    ...checkinForm,
    objectiveId: currentObjective.value!.id!,
    userId: 0, // 实际项目中从 store/session 获取当前用户 ID
  })
  ElMessage.success('Check-in 提交成功')
  checkinVisible.value = false
  await loadCheckins()
}

// ==================== 创建目标 ====================
const createObjectiveVisible = ref(false)
const createObjectiveParentId = ref<number | null>(null)
const createObjectiveForm = reactive({
  title: '',
  description: '',
  ownerId: 0,
  ownerType: 'USER',
  weight: 100,
})

function openCreateObjectiveDialog(parentId: number | null | undefined) {
  createObjectiveParentId.value = parentId ?? null
  Object.assign(createObjectiveForm, {
    title: '',
    description: '',
    ownerId: 0,
    ownerType: 'USER',
    weight: 100,
  })
  createObjectiveVisible.value = true
}

async function submitCreateObjective() {
  if (!createObjectiveForm.title) {
    ElMessage.warning('请填写目标标题')
    return
  }
  if (!selectedPeriodId.value) {
    ElMessage.warning('请先选择周期')
    return
  }
  await okrObjectiveApi.create({
    ...createObjectiveForm,
    periodId: selectedPeriodId.value,
    parentId: createObjectiveParentId.value ?? undefined,
  })
  ElMessage.success('目标创建成功')
  createObjectiveVisible.value = false
  await loadObjectiveTree()
}

// ==================== AI 建议 KR ====================
const suggestKrVisible = ref(false)
const suggestLoading = ref(false)
const suggestedKrs = ref<string[]>([])
const selectedSuggestions = ref<boolean[]>([])

async function openSuggestKrDialog() {
  if (!currentObjective.value) return
  suggestKrVisible.value = true
  suggestLoading.value = true
  suggestedKrs.value = []
  selectedSuggestions.value = []
  try {
    const res = await okrObjectiveApi.suggestKr(
      currentObjective.value.id!,
      currentObjective.value.title,
      currentObjective.value.description,
    )
    suggestedKrs.value = (res as any).data || res || []
    selectedSuggestions.value = suggestedKrs.value.map(() => true)
  } catch {
    ElMessage.warning('AI 建议生成失败')
  } finally {
    suggestLoading.value = false
  }
}

async function adoptSuggestedKrs() {
  const selected = suggestedKrs.value.filter((_, i) => selectedSuggestions.value[i])
  if (selected.length === 0) {
    ElMessage.warning('请至少勾选一条建议')
    return
  }
  for (const title of selected) {
    await okrKeyResultApi.create({
      objectiveId: currentObjective.value!.id!,
      title,
      measureType: 'MILESTONE',
      weight: 25,
    } as OkrKeyResult)
  }
  ElMessage.success(`已采用 ${selected.length} 条 KR 建议`)
  suggestKrVisible.value = false
  await loadKeyResults()
}

// ==================== 工具函数 ====================
function statusTagType(status?: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return 'primary'
}

function statusLabel(status?: number) {
  if (status === 1) return '已完成'
  if (status === 2) return '已取消'
  return '进行中'
}

function progressColor(progress?: number) {
  if (!progress) return '#909399'
  if (progress >= 80) return '#67C23A'
  if (progress >= 50) return '#E6A23C'
  return '#F56C6C'
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadPeriods()
})
</script>

<style scoped>
.okr-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
  box-sizing: border-box;
}

.okr-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.period-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  font-weight: 500;
  color: #303133;
}

.okr-body {
  display: flex;
  flex: 1;
  gap: 16px;
  overflow: hidden;
}

.objective-tree-panel {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  overflow-y: auto;
}

.panel-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  margin-bottom: 10px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.node-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.objective-detail-panel {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.objective-detail-panel.no-selection {
  justify-content: center;
  align-items: center;
}

.objective-info-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}

.objective-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
}

.objective-desc {
  color: #606266;
  font-size: 13px;
  margin: 6px 0 0;
}

.objective-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.kr-section,
.checkin-section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}

.section-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kr-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 8px;
}

.kr-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.kr-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.kr-actions {
  margin-top: 4px;
}

.empty-tip {
  color: #c0c4cc;
  font-size: 13px;
  text-align: center;
  padding: 16px 0;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
  color: #303133;
}
</style>
