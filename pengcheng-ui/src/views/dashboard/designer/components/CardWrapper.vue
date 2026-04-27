<template>
  <div class="card-wrapper" :class="{ 'card-wrapper--editable': editable, 'card-wrapper--loading': loading, 'card-wrapper--error': !!error }">
    <!-- 编辑模式工具栏 -->
    <div v-if="editable" class="card-wrapper__toolbar">
      <div class="card-wrapper__drag-handle" title="拖拽移动">⠿</div>
      <span class="card-wrapper__name">{{ meta.name }}</span>
      <button class="card-wrapper__delete" title="删除卡片" @click.stop="emit('delete')">✕</button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="card-wrapper__placeholder card-wrapper__placeholder--loading">
      <n-spin size="small" />
      <span>加载中…</span>
    </div>

    <!-- 错误 -->
    <div v-else-if="error" class="card-wrapper__placeholder card-wrapper__placeholder--error">
      <n-icon size="24" color="#d03050"><WarningOutline /></n-icon>
      <span>{{ error }}</span>
    </div>

    <!-- 内容区 -->
    <div v-else class="card-wrapper__content">
      <!-- 大数字卡片 -->
      <template v-if="meta.suggestedChart === 'number'">
        <div class="card-number">
          <div class="card-number__value">{{ formatNumber(data) }}</div>
          <div class="card-number__label">{{ meta.name }}</div>
        </div>
      </template>

      <!-- 表格卡片 -->
      <template v-else-if="meta.suggestedChart === 'table'">
        <n-data-table
          v-if="Array.isArray(data) && data.length"
          :columns="buildTableColumns(data[0])"
          :data="data"
          :pagination="false"
          size="small"
          :bordered="false"
          :max-height="180"
        />
        <div v-else class="card-wrapper__placeholder">暂无数据</div>
      </template>

      <!-- ECharts 类图表 -->
      <template v-else-if="isChartType(meta.suggestedChart)">
        <div v-if="hasEcharts" ref="chartEl" class="card-chart-area" />
        <!-- ECharts 未集成时的降级展示 -->
        <div v-else class="card-chart-fallback">
          <div class="card-chart-fallback__type">{{ meta.suggestedChart }} 图表</div>
          <pre class="card-chart-fallback__data">{{ JSON.stringify(data, null, 2) }}</pre>
          <!-- TODO: 集成 echarts / vue-echarts 后替换此占位 -->
        </div>
      </template>

      <!-- 未知类型兜底 -->
      <template v-else>
        <pre class="card-wrapper__raw">{{ JSON.stringify(data, null, 2) }}</pre>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { NSpin, NIcon, NDataTable } from 'naive-ui'
import { WarningOutline } from '@vicons/ionicons5'
import type { CardMeta } from '@/api/dashboardCard'

// ─── Props / Emits ────────────────────────────────────────────────────────────

const props = defineProps<{
  meta: CardMeta
  data: unknown | null
  loading: boolean
  error: string | null
  editable: boolean
}>()

const emit = defineEmits<{
  (e: 'delete'): void
}>()

// ─── ECharts 动态集成 ─────────────────────────────────────────────────────────

const CHART_TYPES = ['line', 'bar', 'pie', 'funnel', 'heatmap', 'gauge']

function isChartType(t: string) {
  return CHART_TYPES.includes(t)
}

// 检测 echarts 是否可用（项目已安装 echarts ^6）
const hasEcharts = ref(false)
let echarts: any = null
const chartEl = ref<HTMLDivElement | null>(null)
let chartInstance: any = null

async function initEcharts() {
  try {
    echarts = await import('echarts')
    hasEcharts.value = true
  } catch {
    hasEcharts.value = false
  }
}

function buildEchartsOption(data: unknown) {
  const type = props.meta.suggestedChart
  if (type === 'number') return null

  // 通用数据适配：后端直接返回 echarts option 则直接用
  if (data && typeof data === 'object' && !Array.isArray(data) && (data as any).series) {
    return data
  }

  // 简单数组 → 基础图表
  if (Array.isArray(data)) {
    const xData = data.map((item: any, i) => item.label ?? item.name ?? item.date ?? String(i))
    const yData = data.map((item: any) => item.value ?? item.count ?? item.amount ?? 0)

    if (type === 'pie' || type === 'funnel') {
      return {
        tooltip: { trigger: 'item' },
        series: [{ type, data: data.map((item: any, i) => ({ name: xData[i], value: yData[i] })) }]
      }
    }

    if (type === 'gauge') {
      const val = typeof data[0] === 'number' ? data[0] : yData[0] ?? 0
      return {
        series: [{ type: 'gauge', data: [{ value: val, name: props.meta.name }] }]
      }
    }

    return {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: xData },
      yAxis: { type: 'value' },
      series: [{ type: type === 'heatmap' ? 'bar' : type, data: yData }]
    }
  }

  return null
}

async function renderChart() {
  if (!hasEcharts.value || !chartEl.value || !props.data) return
  await nextTick()
  if (!chartInstance) {
    chartInstance = echarts.init(chartEl.value)
  }
  const option = buildEchartsOption(props.data)
  if (option) chartInstance.setOption(option, true)
}

function disposeChart() {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

const resizeObserver = ref<ResizeObserver | null>(null)

onMounted(async () => {
  await initEcharts()
  if (isChartType(props.meta.suggestedChart)) {
    await renderChart()
    if (chartEl.value) {
      resizeObserver.value = new ResizeObserver(() => chartInstance?.resize())
      resizeObserver.value.observe(chartEl.value)
    }
  }
})

onUnmounted(() => {
  resizeObserver.value?.disconnect()
  disposeChart()
})

watch(
  () => [props.data, props.meta.suggestedChart],
  async () => {
    if (isChartType(props.meta.suggestedChart) && !props.loading && !props.error) {
      await renderChart()
    }
  }
)

// ─── 大数字格式化 ──────────────────────────────────────────────────────────────

function formatNumber(raw: unknown): string {
  if (raw === null || raw === undefined) return '-'
  const n = typeof raw === 'object' ? (raw as any).value ?? (raw as any).count ?? 0 : Number(raw)
  if (isNaN(n)) return String(raw)
  if (n >= 1e8) return (n / 1e8).toFixed(2) + '亿'
  if (n >= 1e4) return (n / 1e4).toFixed(1) + '万'
  return n.toLocaleString('zh-CN')
}

// ─── 表格列自动推导 ────────────────────────────────────────────────────────────

function buildTableColumns(row: Record<string, unknown>) {
  return Object.keys(row).map(key => ({
    title: key,
    key,
    ellipsis: { tooltip: true }
  }))
}
</script>

<style scoped>
.card-wrapper {
  position: relative;
  height: 100%;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.2s;
}

.card-wrapper--editable {
  box-shadow: 0 2px 8px rgba(24, 160, 88, 0.18);
  border: 1px solid #d0ead9;
}

.card-wrapper--editable:hover {
  box-shadow: 0 4px 16px rgba(24, 160, 88, 0.28);
}

/* 工具栏 */
.card-wrapper__toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: #f8fff8;
  border-bottom: 1px solid #e8f5e9;
  font-size: 12px;
}

.card-wrapper__drag-handle {
  cursor: grab;
  color: #aaa;
  font-size: 16px;
  line-height: 1;
  user-select: none;
}

.card-wrapper__drag-handle:active {
  cursor: grabbing;
}

.card-wrapper__name {
  flex: 1;
  color: #555;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-wrapper__delete {
  cursor: pointer;
  color: #d03050;
  background: none;
  border: none;
  font-size: 14px;
  padding: 0 2px;
  line-height: 1;
}

.card-wrapper__delete:hover {
  opacity: 0.7;
}

/* 内容区 */
.card-wrapper__content {
  flex: 1;
  overflow: auto;
  padding: 12px;
}

/* 占位 / 状态 */
.card-wrapper__placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  color: #999;
  font-size: 13px;
  padding: 16px;
}

.card-wrapper__placeholder--error {
  color: #d03050;
}

/* 大数字 */
.card-number {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
}

.card-number__value {
  font-size: 36px;
  font-weight: 700;
  color: #18a058;
  line-height: 1;
}

.card-number__label {
  font-size: 13px;
  color: #999;
}

/* 图表区 */
.card-chart-area {
  width: 100%;
  height: 180px;
}

.card-chart-fallback {
  padding: 8px;
}

.card-chart-fallback__type {
  font-size: 12px;
  color: #aaa;
  margin-bottom: 4px;
  font-style: italic;
}

.card-chart-fallback__data {
  font-size: 11px;
  color: #666;
  max-height: 140px;
  overflow: auto;
  background: #f5f5f5;
  border-radius: 4px;
  padding: 6px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 原始数据兜底 */
.card-wrapper__raw {
  font-size: 11px;
  color: #666;
  max-height: 160px;
  overflow: auto;
  background: #f5f5f5;
  border-radius: 4px;
  padding: 6px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
