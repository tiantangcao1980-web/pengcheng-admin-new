<template>
  <div class="unit-board">
    <!-- 顶部筛选栏 -->
    <n-card class="filter-bar">
      <n-space align="center" wrap>
        <n-form-item label="楼盘" :show-feedback="false">
          <n-select
            v-model:value="selectedProjectId"
            :options="projectOptions"
            label-field="projectName"
            value-field="id"
            placeholder="请选择楼盘"
            filterable
            remote
            @search="handleProjectSearch"
            style="width: 220px"
            @update:value="onProjectChange"
          />
        </n-form-item>

        <!-- 统计徽章 -->
        <template v-if="selectedProjectId">
          <n-tag type="success">可售 {{ stats.available }}</n-tag>
          <n-tag type="warning">预留 {{ stats.reserved }}</n-tag>
          <n-tag type="info">已认购 {{ stats.subscribed }}</n-tag>
          <n-tag type="primary">已签约 {{ stats.signed }}</n-tag>
          <n-tag>已售 {{ stats.sold }}</n-tag>
          <n-tag type="error">不可售 {{ stats.unavailable }}</n-tag>
          <n-divider vertical />
          <span class="depletion-rate">
            去化率：<strong>{{ depletionRate }}%</strong>
            （{{ stats.sold + stats.signed + stats.subscribed }} / {{ stats.total }}）
          </span>
        </template>
      </n-space>
    </n-card>

    <!-- 楼栋切换 Tab -->
    <n-card v-if="matrixData.length > 0" style="margin-top: 12px">
      <n-tabs v-model:value="activeBuilding" type="segment">
        <n-tab-pane
          v-for="bld in matrixData"
          :key="bld.building"
          :name="bld.building"
          :tab="`${bld.building} 号楼`"
        >
          <!-- 房源矩阵 -->
          <div class="matrix-wrapper">
            <div
              v-for="row in bld.floors"
              :key="row.floor"
              class="floor-row"
            >
              <!-- 楼层标签 -->
              <div class="floor-label">{{ row.floor }} F</div>
              <!-- 房间色块 -->
              <div class="units-row">
                <div
                  v-for="unit in row.units"
                  :key="unit.id"
                  class="unit-cell"
                  :class="statusClass(unit.status)"
                  :title="`${unit.fullNo}  ${statusLabel(unit.status)}  ${unit.listPrice ? formatPrice(unit.listPrice) : ''}`"
                  @click="openDetail(unit)"
                >
                  <span class="unit-no">{{ unit.unitNo }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 图例 -->
          <n-space class="legend" style="margin-top: 16px" wrap>
            <span v-for="item in LEGEND" :key="item.status" class="legend-item">
              <span class="legend-dot" :class="item.cls"></span>{{ item.label }}
            </span>
          </n-space>
        </n-tab-pane>
      </n-tabs>
    </n-card>

    <n-empty v-else-if="selectedProjectId && !loading" description="暂无房源数据" style="margin-top: 40px" />

    <!-- 房源详情 Drawer -->
    <n-drawer v-model:show="drawerVisible" :width="420" placement="right">
      <n-drawer-content :title="`房源详情 — ${currentUnit?.fullNo}`" closable>
        <template v-if="currentUnit">
          <n-descriptions :column="2" bordered size="small">
            <n-descriptions-item label="楼盘编号">{{ currentUnit.fullNo }}</n-descriptions-item>
            <n-descriptions-item label="当前状态">
              <n-tag :type="tagType(currentUnit.status)">{{ statusLabel(currentUnit.status) }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="建筑面积">{{ currentUnit.area }} m²</n-descriptions-item>
            <n-descriptions-item label="挂牌价">{{ formatPrice(currentUnit.listPrice) }}</n-descriptions-item>
            <n-descriptions-item v-if="currentUnit.actualPrice" label="成交价">
              {{ formatPrice(currentUnit.actualPrice) }}
            </n-descriptions-item>
            <n-descriptions-item v-if="currentUnit.lockedUntil" label="锁定到期">
              {{ currentUnit.lockedUntil }}
            </n-descriptions-item>
            <n-descriptions-item v-if="currentUnit.remark" label="备注" :span="2">
              {{ currentUnit.remark }}
            </n-descriptions-item>
          </n-descriptions>

          <!-- 状态变更操作 -->
          <n-divider>状态操作</n-divider>
          <n-space wrap>
            <n-button
              v-for="action in availableActions(currentUnit.status)"
              :key="action.to"
              :type="action.btnType"
              size="small"
              @click="doChangeStatus(action.to)"
            >
              {{ action.label }}
            </n-button>

            <!-- 锁定 / 解锁 -->
            <n-button
              v-if="currentUnit.status === 'AVAILABLE'"
              type="warning"
              size="small"
              @click="doLock"
            >
              锁定 2h
            </n-button>
            <n-button
              v-if="currentUnit.lockedBy"
              size="small"
              @click="doUnlock"
            >
              解锁
            </n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { unitApi, type RealtyUnit, type UnitMatrix, type UnitStatus } from '@/api/realtyUnit'
import { request } from '@/utils/request'

const message = useMessage()

// ---------- 状态 ----------
const selectedProjectId = ref<number | null>(null)
const projectOptions = ref<{ id: number; projectName: string }[]>([])
const matrixData = ref<UnitMatrix[]>([])
const activeBuilding = ref<string>('')
const loading = ref(false)
const drawerVisible = ref(false)
const currentUnit = ref<RealtyUnit | null>(null)

// ---------- 图例配置 ----------
const LEGEND = [
  { status: 'AVAILABLE',   label: '可售',   cls: 'dot-available' },
  { status: 'RESERVED',    label: '预留',   cls: 'dot-reserved' },
  { status: 'SUBSCRIBED',  label: '已认购', cls: 'dot-subscribed' },
  { status: 'SIGNED',      label: '已签约', cls: 'dot-signed' },
  { status: 'SOLD',        label: '已售',   cls: 'dot-sold' },
  { status: 'UNAVAILABLE', label: '不可售', cls: 'dot-unavailable' },
]

// 状态操作映射
const ACTIONS: Record<string, { to: UnitStatus; label: string; btnType: 'default' | 'primary' | 'success' | 'warning' | 'error' }[]> = {
  AVAILABLE:  [{ to: 'RESERVED', label: '→ 预留', btnType: 'warning' }, { to: 'UNAVAILABLE', label: '下架', btnType: 'error' }],
  RESERVED:   [{ to: 'SUBSCRIBED', label: '→ 认购', btnType: 'primary' }, { to: 'AVAILABLE', label: '撤销预留', btnType: 'default' }, { to: 'UNAVAILABLE', label: '下架', btnType: 'error' }],
  SUBSCRIBED: [{ to: 'SIGNED', label: '→ 签约', btnType: 'success' }, { to: 'AVAILABLE', label: '撤销认购', btnType: 'default' }, { to: 'UNAVAILABLE', label: '下架', btnType: 'error' }],
  SIGNED:     [{ to: 'SOLD', label: '→ 交房', btnType: 'success' }, { to: 'UNAVAILABLE', label: '下架', btnType: 'error' }],
  SOLD:       [{ to: 'UNAVAILABLE', label: '下架', btnType: 'error' }],
  UNAVAILABLE:[{ to: 'AVAILABLE', label: '重新上架', btnType: 'primary' }],
}

// ---------- 统计 ----------
const stats = computed(() => {
  const all = matrixData.value.flatMap(b => b.floors.flatMap(f => f.units))
  const count = (s: string) => all.filter(u => u.status === s).length
  return {
    available:   count('AVAILABLE'),
    reserved:    count('RESERVED'),
    subscribed:  count('SUBSCRIBED'),
    signed:      count('SIGNED'),
    sold:        count('SOLD'),
    unavailable: count('UNAVAILABLE'),
    total:       all.length,
  }
})

/**
 * 去化率 = (SOLD + SIGNED + SUBSCRIBED) / total * 100
 */
const depletionRate = computed(() => {
  const { sold, signed, subscribed, total } = stats.value
  if (total === 0) return '0.0'
  return ((sold + signed + subscribed) / total * 100).toFixed(1)
})

// ---------- 工具函数 ----------
const statusClass = (status?: string) => ({
  'cell-available':   status === 'AVAILABLE',
  'cell-reserved':    status === 'RESERVED',
  'cell-subscribed':  status === 'SUBSCRIBED',
  'cell-signed':      status === 'SIGNED',
  'cell-sold':        status === 'SOLD',
  'cell-unavailable': status === 'UNAVAILABLE',
})

const STATUS_LABELS: Record<string, string> = {
  AVAILABLE: '可售', RESERVED: '预留', SUBSCRIBED: '已认购',
  SIGNED: '已签约', SOLD: '已售', UNAVAILABLE: '不可售',
}
const statusLabel = (s?: string) => STATUS_LABELS[s ?? ''] ?? s ?? '-'

const tagType = (s?: string): 'success' | 'warning' | 'info' | 'default' | 'error' | 'primary' => {
  const map: Record<string, any> = {
    AVAILABLE: 'success', RESERVED: 'warning', SUBSCRIBED: 'info',
    SIGNED: 'primary', SOLD: 'default', UNAVAILABLE: 'error',
  }
  return map[s ?? ''] ?? 'default'
}

const formatPrice = (price?: number) => {
  if (price == null) return '-'
  return price >= 10000 ? `${(price / 10000).toFixed(2)} 万` : `${price} 元`
}

const availableActions = (status?: string) => ACTIONS[status ?? ''] ?? []

// ---------- 数据加载 ----------
const handleProjectSearch = async (keyword: string) => {
  if (!keyword) return
  try {
    const res = await request.get<any[]>('/admin/project/search', { params: { keyword } })
    projectOptions.value = res as any
  } catch { /* ignore */ }
}

const onProjectChange = async (id: number | null) => {
  if (!id) { matrixData.value = []; return }
  loading.value = true
  try {
    const data = await unitApi.matrix(id) as unknown as UnitMatrix[]
    matrixData.value = data
    if (data.length > 0) activeBuilding.value = data[0].building
  } catch (e: any) {
    message.error('加载房源矩阵失败')
  } finally {
    loading.value = false
  }
}

// ---------- Drawer 操作 ----------
const openDetail = (unit: RealtyUnit) => {
  currentUnit.value = { ...unit }
  drawerVisible.value = true
}

const doChangeStatus = async (toStatus: UnitStatus) => {
  if (!currentUnit.value?.id) return
  try {
    await unitApi.changeStatus(currentUnit.value.id, { toStatus })
    message.success('状态更新成功')
    drawerVisible.value = false
    await onProjectChange(selectedProjectId.value)
  } catch (e: any) {
    message.error(e?.message ?? '操作失败')
  }
}

const doLock = async () => {
  if (!currentUnit.value?.id) return
  try {
    const ok = await unitApi.lock(currentUnit.value.id, { userId: 0, hours: 2 }) as unknown as boolean
    ok ? message.success('锁定成功') : message.warning('锁定失败，房源已被他人锁定')
    await onProjectChange(selectedProjectId.value)
  } catch { message.error('锁定操作失败') }
}

const doUnlock = async () => {
  if (!currentUnit.value?.id) return
  try {
    await unitApi.unlock(currentUnit.value.id)
    message.success('解锁成功')
    await onProjectChange(selectedProjectId.value)
  } catch { message.error('解锁操作失败') }
}
</script>

<style scoped>
.unit-board {
  padding: 16px;
}
.filter-bar {
  margin-bottom: 8px;
}
.depletion-rate {
  font-size: 14px;
  color: #333;
}
.matrix-wrapper {
  overflow-x: auto;
}
.floor-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
.floor-label {
  width: 48px;
  font-size: 13px;
  font-weight: 600;
  color: #555;
  flex-shrink: 0;
  text-align: right;
  padding-right: 10px;
}
.units-row {
  display: flex;
  gap: 6px;
  flex-wrap: nowrap;
}
.unit-cell {
  width: 60px;
  height: 44px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.1s, box-shadow 0.1s;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  user-select: none;
}
.unit-cell:hover {
  transform: scale(1.08);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}
.cell-available   { background: #18a058; }
.cell-reserved    { background: #f0a020; }
.cell-subscribed  { background: #e07d17; }
.cell-signed      { background: #2080f0; }
.cell-sold        { background: #888; }
.cell-unavailable { background: #d03050; }

/* 图例 */
.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #555;
}
.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}
.dot-available   { background: #18a058; }
.dot-reserved    { background: #f0a020; }
.dot-subscribed  { background: #e07d17; }
.dot-signed      { background: #2080f0; }
.dot-sold        { background: #888; }
.dot-unavailable { background: #d03050; }
</style>
