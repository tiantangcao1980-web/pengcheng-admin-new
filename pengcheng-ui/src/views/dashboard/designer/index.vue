<template>
  <div class="designer" :class="{ 'designer--preview': previewMode }">
    <!-- ── 顶部工具栏 ── -->
    <div class="designer__toolbar">
      <div class="designer__toolbar-left">
        <span class="designer__title">看板编辑器</span>
        <n-button-group size="small">
          <n-button
            v-for="opt in ownerTypeOptions"
            :key="opt.value"
            :type="ownerType === opt.value ? 'primary' : 'default'"
            @click="switchOwnerType(opt.value)"
          >{{ opt.label }}</n-button>
        </n-button-group>
      </div>
      <div class="designer__toolbar-right">
        <n-tag v-if="isDirty" type="warning" size="small">未保存</n-tag>
        <n-button size="small" @click="previewMode = !previewMode">
          {{ previewMode ? '退出预览' : '预览' }}
        </n-button>
        <n-button size="small" @click="resetLayout">重置</n-button>
        <n-button size="small" type="primary" :loading="saving" @click="handleSave">保存</n-button>
      </div>
    </div>

    <div class="designer__body">
      <!-- ── 左侧卡片市场 ── -->
      <div v-show="!previewMode" class="designer__sidebar">
        <div class="designer__sidebar-title">卡片市场</div>
        <div v-if="cardsLoading" class="designer__sidebar-loading">
          <n-spin size="small" />
        </div>
        <template v-else>
          <n-collapse v-if="cardsByCategory.length" :default-expanded-names="cardsByCategory.map(g => g.category)">
            <n-collapse-item
              v-for="group in cardsByCategory"
              :key="group.category"
              :title="group.category"
              :name="group.category"
            >
              <div class="designer__card-list">
                <div
                  v-for="card in group.cards"
                  :key="card.code"
                  class="designer__card-item"
                  :title="card.description || card.name"
                  @click="addCardToCanvas(card)"
                >
                  <div class="designer__card-item-icon">{{ chartIcon(card.suggestedChart) }}</div>
                  <div class="designer__card-item-info">
                    <div class="designer__card-item-name">{{ card.name }}</div>
                    <div class="designer__card-item-type">{{ card.suggestedChart }}</div>
                  </div>
                </div>
              </div>
            </n-collapse-item>
          </n-collapse>
          <n-empty v-else description="暂无卡片" size="small" />
        </template>
      </div>

      <!-- ── 中央画布 ── -->
      <div class="designer__canvas-area" ref="canvasAreaEl">
        <div v-if="canvasItems.length === 0 && !previewMode" class="designer__canvas-empty">
          <n-empty description="从左侧点击卡片添加到看板" />
        </div>
        <!-- 12 列 CSS Grid 画布 -->
        <div
          class="designer__grid"
          :style="{ '--grid-cols': COLS }"
        >
          <div
            v-for="(item, idx) in canvasItems"
            :key="item.cardCode + '-' + idx"
            class="designer__grid-item"
            :style="gridItemStyle(item)"
            :data-idx="idx"
          >
            <CardWrapper
              :meta="getCardMeta(item.cardCode)"
              :data="cardDataMap[item.cardCode] ?? null"
              :loading="cardLoadingMap[item.cardCode] ?? false"
              :error="cardErrorMap[item.cardCode] ?? null"
              :editable="!previewMode"
              @delete="removeCard(idx)"
            />
            <!-- 边角拉伸手柄（编辑模式）-->
            <div
              v-if="!previewMode"
              class="designer__resize-handle"
              @mousedown.stop="startResize($event, idx)"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  NButtonGroup, NButton, NCollapse, NCollapseItem,
  NSpin, NEmpty, NTag, useMessage
} from 'naive-ui'
import CardWrapper from './components/CardWrapper.vue'
import {
  listCards, renderCard, getDefaultLayout, saveLayout,
  type CardMeta, type LayoutItem, type Layout
} from '@/api/dashboardCard'

const msg = useMessage()

// ─── 常量 ─────────────────────────────────────────────────────────────────────
const COLS = 12
const DEFAULT_W = 4
const DEFAULT_H = 3
const ROW_H = 80   // px per row unit (approximate, for grid display)

// ─── 状态 ─────────────────────────────────────────────────────────────────────
const previewMode = ref(false)
const saving = ref(false)
const isDirty = ref(false)
const ownerType = ref<'user' | 'role' | 'global'>('user')
const canvasItems = ref<LayoutItem[]>([])

const allCards = ref<CardMeta[]>([])
const cardsLoading = ref(false)

// cardCode → render result / loading / error
const cardDataMap = ref<Record<string, unknown>>({})
const cardLoadingMap = ref<Record<string, boolean>>({})
const cardErrorMap = ref<Record<string, string | null>>({})

const ownerTypeOptions = [
  { label: '我的', value: 'user' as const },
  { label: '角色', value: 'role' as const },
  { label: '全局', value: 'global' as const }
]

// ─── 计算 ─────────────────────────────────────────────────────────────────────

const cardsByCategory = computed(() => {
  const map = new Map<string, CardMeta[]>()
  for (const c of allCards.value) {
    if (!map.has(c.category)) map.set(c.category, [])
    map.get(c.category)!.push(c)
  }
  return Array.from(map.entries()).map(([category, cards]) => ({ category, cards }))
})

function getCardMeta(code: string): CardMeta {
  return allCards.value.find(c => c.code === code) ?? {
    code,
    name: code,
    category: '',
    suggestedChart: 'number',
    defaultCols: DEFAULT_W,
    defaultRows: DEFAULT_H
  }
}

// ─── 布局辅助 ─────────────────────────────────────────────────────────────────

function gridItemStyle(item: LayoutItem) {
  return {
    gridColumn: `${item.x + 1} / span ${item.w}`,
    gridRow: `${item.y + 1} / span ${item.h}`,
    minHeight: `${item.h * ROW_H}px`
  }
}

/** 在画布中寻找最近的空行来放置新卡片 */
function findNextPosition(w: number, h: number): { x: number; y: number } {
  // 简单策略：从行 0 向下遍历，找到能放下 w 列的空位
  const occupied = new Set<string>()
  for (const item of canvasItems.value) {
    for (let r = item.y; r < item.y + item.h; r++) {
      for (let c = item.x; c < item.x + item.w; c++) {
        occupied.add(`${r},${c}`)
      }
    }
  }

  for (let row = 0; row < 100; row++) {
    for (let col = 0; col <= COLS - w; col++) {
      let fits = true
      outer: for (let r = row; r < row + h; r++) {
        for (let c = col; c < col + w; c++) {
          if (occupied.has(`${r},${c}`)) { fits = false; break outer }
        }
      }
      if (fits) return { x: col, y: row }
    }
  }
  return { x: 0, y: 0 }
}

// ─── 卡片操作 ─────────────────────────────────────────────────────────────────

function addCardToCanvas(card: CardMeta) {
  const w = card.defaultCols ?? DEFAULT_W
  const h = card.defaultRows ?? DEFAULT_H
  const { x, y } = findNextPosition(w, h)
  canvasItems.value.push({ cardCode: card.code, x, y, w, h })
  isDirty.value = true
  fetchCardData(card.code)
}

function removeCard(idx: number) {
  canvasItems.value.splice(idx, 1)
  isDirty.value = true
}

// ─── 渲染数据 ─────────────────────────────────────────────────────────────────

async function fetchCardData(code: string, body: Record<string, string> = {}) {
  cardLoadingMap.value[code] = true
  cardErrorMap.value[code] = null
  try {
    const result = await renderCard(code, body)
    cardDataMap.value[code] = result
  } catch (e: any) {
    cardErrorMap.value[code] = e?.message || '加载失败'
  } finally {
    cardLoadingMap.value[code] = false
  }
}

function fetchAllCanvasData() {
  const codes = [...new Set(canvasItems.value.map(i => i.cardCode))]
  codes.forEach(code => fetchCardData(code))
}

// ─── 布局持久化 ───────────────────────────────────────────────────────────────

async function loadLayout() {
  try {
    const layout = await getDefaultLayout({ ownerType: ownerType.value })
    if (layout?.layoutJson) {
      canvasItems.value = layout.layoutJson
      fetchAllCanvasData()
    }
  } catch {
    // 首次使用无布局，静默处理
  }
}

async function handleSave() {
  saving.value = true
  try {
    await saveLayout({
      ownerType: ownerType.value,
      layoutJson: canvasItems.value,
      isDefault: true
    })
    isDirty.value = false
    msg.success('保存成功')
  } catch (e: any) {
    msg.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function resetLayout() {
  canvasItems.value = []
  isDirty.value = false
}

function switchOwnerType(type: 'user' | 'role' | 'global') {
  ownerType.value = type
  canvasItems.value = []
  cardDataMap.value = {}
  loadLayout()
}

// ─── 拖拽换位（简易实现） ─────────────────────────────────────────────────────

const canvasAreaEl = ref<HTMLElement | null>(null)
let dragIdx = -1
let dragStartX = 0
let dragStartY = 0
let dragOrigItem: LayoutItem | null = null

// 注：vue-grid-layout-next 未安装时用简易交换逻辑
// 如需完整拖拽，可在 package.json 追加 "vue-grid-layout-next": "^1.0.0"
// 此处为轻量 swap 实现
function startCardDrag(event: MouseEvent, idx: number) {
  dragIdx = idx
  dragStartX = event.clientX
  dragStartY = event.clientY
  dragOrigItem = { ...canvasItems.value[idx] }
  window.addEventListener('mousemove', onCardDragMove)
  window.addEventListener('mouseup', onCardDragEnd)
}

function onCardDragMove(event: MouseEvent) {
  if (dragIdx < 0 || !dragOrigItem) return
  const deltaX = event.clientX - dragStartX
  const deltaY = event.clientY - dragStartY
  const colW = (canvasAreaEl.value?.clientWidth ?? 960) / COLS
  const dCol = Math.round(deltaX / colW)
  const dRow = Math.round(deltaY / ROW_H)
  const newX = Math.max(0, Math.min(COLS - dragOrigItem.w, dragOrigItem.x + dCol))
  const newY = Math.max(0, dragOrigItem.y + dRow)
  canvasItems.value[dragIdx] = { ...dragOrigItem, x: newX, y: newY }
}

function onCardDragEnd() {
  dragIdx = -1
  dragOrigItem = null
  isDirty.value = true
  window.removeEventListener('mousemove', onCardDragMove)
  window.removeEventListener('mouseup', onCardDragEnd)
}

// ─── 调整大小 ─────────────────────────────────────────────────────────────────

let resizeIdx = -1
let resizeStartX = 0
let resizeStartY = 0
let resizeOrigItem: LayoutItem | null = null

function startResize(event: MouseEvent, idx: number) {
  resizeIdx = idx
  resizeStartX = event.clientX
  resizeStartY = event.clientY
  resizeOrigItem = { ...canvasItems.value[idx] }
  window.addEventListener('mousemove', onResizeMove)
  window.addEventListener('mouseup', onResizeEnd)
}

function onResizeMove(event: MouseEvent) {
  if (resizeIdx < 0 || !resizeOrigItem) return
  const deltaX = event.clientX - resizeStartX
  const deltaY = event.clientY - resizeStartY
  const colW = (canvasAreaEl.value?.clientWidth ?? 960) / COLS
  const dCol = Math.round(deltaX / colW)
  const dRow = Math.round(deltaY / ROW_H)
  const newW = Math.max(2, Math.min(COLS - resizeOrigItem.x, resizeOrigItem.w + dCol))
  const newH = Math.max(2, resizeOrigItem.h + dRow)
  canvasItems.value[resizeIdx] = { ...resizeOrigItem, w: newW, h: newH }
}

function onResizeEnd() {
  resizeIdx = -1
  resizeOrigItem = null
  isDirty.value = true
  window.removeEventListener('mousemove', onResizeMove)
  window.removeEventListener('mouseup', onResizeEnd)
}

// ─── 图表图标辅助 ─────────────────────────────────────────────────────────────

function chartIcon(type: string) {
  const map: Record<string, string> = {
    number: '🔢', line: '📈', bar: '📊', pie: '🥧',
    funnel: '📉', heatmap: '🌡️', gauge: '⏱️', table: '📋'
  }
  return map[type] ?? '📦'
}

// ─── 生命周期 ─────────────────────────────────────────────────────────────────

onMounted(async () => {
  cardsLoading.value = true
  try {
    allCards.value = await listCards()
  } catch {
    allCards.value = []
  } finally {
    cardsLoading.value = false
  }
  await loadLayout()
})
</script>

<style scoped>
.designer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  overflow: hidden;
}

/* 工具栏 */
.designer__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  gap: 12px;
  flex-shrink: 0;
}

.designer__toolbar-left,
.designer__toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.designer__title {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}

/* 主体 */
.designer__body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 左侧卡片市场 */
.designer__sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  overflow-y: auto;
  flex-shrink: 0;
  padding: 12px 8px;
}

.designer__sidebar-title {
  font-weight: 600;
  font-size: 13px;
  color: #333;
  padding: 0 8px 10px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
}

.designer__sidebar-loading {
  display: flex;
  justify-content: center;
  padding: 24px;
}

.designer__card-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 0;
}

.designer__card-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.designer__card-item:hover {
  background: #f0faf4;
}

.designer__card-item-icon {
  font-size: 18px;
  flex-shrink: 0;
  line-height: 1;
}

.designer__card-item-name {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.designer__card-item-type {
  font-size: 11px;
  color: #aaa;
}

/* 画布区 */
.designer__canvas-area {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.designer__canvas-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
}

/* CSS Grid 画布 */
.designer__grid {
  display: grid;
  grid-template-columns: repeat(var(--grid-cols), 1fr);
  gap: 12px;
  min-height: 400px;
}

/* 单个卡片格子 */
.designer__grid-item {
  position: relative;
  user-select: none;
}

/* 拉伸手柄 */
.designer__resize-handle {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 16px;
  height: 16px;
  cursor: se-resize;
  background: linear-gradient(135deg, transparent 50%, #18a058 50%);
  border-radius: 0 0 8px 0;
  opacity: 0;
  transition: opacity 0.15s;
}

.designer__grid-item:hover .designer__resize-handle {
  opacity: 0.7;
}

/* 预览模式隐藏手柄 */
.designer--preview .designer__grid-item .designer__resize-handle {
  display: none;
}
</style>
