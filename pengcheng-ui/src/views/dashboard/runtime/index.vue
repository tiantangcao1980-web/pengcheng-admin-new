<template>
  <div class="runtime">
    <!-- 顶部时间窗选择 -->
    <div class="runtime__header">
      <span class="runtime__title">数据看板</span>
      <n-button-group size="small">
        <n-button
          v-for="opt in windowOptions"
          :key="opt.value"
          :type="selectedWindow === opt.value ? 'primary' : 'default'"
          @click="selectWindow(opt.value)"
        >{{ opt.label }}</n-button>
      </n-button-group>
      <n-date-picker
        v-if="selectedWindow === 'custom'"
        v-model:value="customRange"
        type="daterange"
        size="small"
        style="width: 240px"
        @update:value="fetchAll"
      />
      <span class="runtime__refresh-hint">自动刷新 {{ countdown }}s</span>
    </div>

    <!-- 画布 -->
    <div class="runtime__canvas">
      <div v-if="loading && canvasItems.length === 0" class="runtime__loading">
        <n-spin size="large" />
        <span>加载看板…</span>
      </div>

      <div v-else-if="canvasItems.length === 0" class="runtime__empty">
        <n-empty description="暂无看板布局，请前往编辑器配置" />
        <n-button type="primary" size="small" style="margin-top: 12px" @click="goDesigner">
          前往编辑器
        </n-button>
      </div>

      <div v-else class="runtime__grid" :style="{ '--grid-cols': COLS }">
        <div
          v-for="(item, idx) in canvasItems"
          :key="item.cardCode + '-' + idx"
          class="runtime__grid-item"
          :style="gridItemStyle(item)"
        >
          <CardWrapper
            :meta="getCardMeta(item.cardCode)"
            :data="cardDataMap[item.cardCode] ?? null"
            :loading="cardLoadingMap[item.cardCode] ?? false"
            :error="cardErrorMap[item.cardCode] ?? null"
            :editable="false"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButtonGroup, NButton, NDatePicker, NSpin, NEmpty } from 'naive-ui'
import CardWrapper from '../designer/components/CardWrapper.vue'
import { getDefaultLayout, listCards, renderCard, type CardMeta, type LayoutItem } from '@/api/dashboardCard'

const router = useRouter()

// ─── 常量 ─────────────────────────────────────────────────────────────────────
const COLS = 12
const ROW_H = 80
const AUTO_REFRESH_SEC = 30

// ─── 时间窗 ───────────────────────────────────────────────────────────────────
type WindowKey = 'today' | 'week' | 'month' | 'custom'

const windowOptions: { label: string; value: WindowKey }[] = [
  { label: '今日', value: 'today' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '自定义', value: 'custom' }
]

const selectedWindow = ref<WindowKey>('today')
const customRange = ref<[number, number] | null>(null)

function getWindowDates(): { windowStart: string; windowEnd: string } {
  const now = new Date()
  const fmt = (d: Date) => d.toISOString().slice(0, 10)

  if (selectedWindow.value === 'custom' && customRange.value) {
    return {
      windowStart: fmt(new Date(customRange.value[0])),
      windowEnd: fmt(new Date(customRange.value[1]))
    }
  }

  if (selectedWindow.value === 'today') {
    const s = fmt(now)
    return { windowStart: s, windowEnd: s }
  }

  if (selectedWindow.value === 'week') {
    const mon = new Date(now)
    mon.setDate(now.getDate() - ((now.getDay() + 6) % 7))
    return { windowStart: fmt(mon), windowEnd: fmt(now) }
  }

  // month
  const first = new Date(now.getFullYear(), now.getMonth(), 1)
  return { windowStart: fmt(first), windowEnd: fmt(now) }
}

// ─── 数据 ─────────────────────────────────────────────────────────────────────
const loading = ref(false)
const allCards = ref<CardMeta[]>([])
const canvasItems = ref<LayoutItem[]>([])
const cardDataMap = ref<Record<string, unknown>>({})
const cardLoadingMap = ref<Record<string, boolean>>({})
const cardErrorMap = ref<Record<string, string | null>>({})

function getCardMeta(code: string): CardMeta {
  return allCards.value.find(c => c.code === code) ?? {
    code,
    name: code,
    category: '',
    suggestedChart: 'number',
    defaultCols: 4,
    defaultRows: 3
  }
}

function gridItemStyle(item: LayoutItem) {
  return {
    gridColumn: `${item.x + 1} / span ${item.w}`,
    gridRow: `${item.y + 1} / span ${item.h}`,
    minHeight: `${item.h * ROW_H}px`
  }
}

async function fetchCardData(code: string) {
  cardLoadingMap.value[code] = true
  cardErrorMap.value[code] = null
  try {
    const result = await renderCard(code, getWindowDates())
    cardDataMap.value[code] = result
  } catch (e: any) {
    cardErrorMap.value[code] = e?.message || '加载失败'
  } finally {
    cardLoadingMap.value[code] = false
  }
}

function fetchAll() {
  const codes = [...new Set(canvasItems.value.map(i => i.cardCode))]
  codes.forEach(code => fetchCardData(code))
}

async function loadLayout() {
  loading.value = true
  try {
    const layout = await getDefaultLayout({ ownerType: 'user' })
    if (layout?.layoutJson) {
      canvasItems.value = layout.layoutJson
      fetchAll()
    }
  } catch {
    // 无布局静默处理
  } finally {
    loading.value = false
  }
}

function selectWindow(w: WindowKey) {
  selectedWindow.value = w
  if (w !== 'custom') fetchAll()
}

function goDesigner() {
  router.push('/dashboard/designer')
}

// ─── 自动刷新 ─────────────────────────────────────────────────────────────────
const countdown = ref(AUTO_REFRESH_SEC)
let timer: ReturnType<typeof setInterval> | null = null

function startRefreshTimer() {
  countdown.value = AUTO_REFRESH_SEC
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      fetchAll()
      countdown.value = AUTO_REFRESH_SEC
    }
  }, 1000)
}

onMounted(async () => {
  try {
    allCards.value = await listCards()
  } catch { /* 静默 */ }
  await loadLayout()
  startRefreshTimer()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.runtime {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  overflow: hidden;
}

.runtime__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.runtime__title {
  font-weight: 600;
  font-size: 15px;
  color: #333;
  margin-right: 4px;
}

.runtime__refresh-hint {
  margin-left: auto;
  font-size: 12px;
  color: #aaa;
}

.runtime__canvas {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.runtime__loading,
.runtime__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 300px;
  color: #999;
}

.runtime__grid {
  display: grid;
  grid-template-columns: repeat(var(--grid-cols), 1fr);
  gap: 12px;
  min-height: 400px;
}

.runtime__grid-item {
  position: relative;
}
</style>
