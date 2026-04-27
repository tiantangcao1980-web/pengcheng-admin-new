<template>
  <view class="project-detail">
    <!-- 头部大图区 -->
    <view class="hero-section">
      <image class="hero-image" :src="project.coverImage || defaultCover" mode="aspectFill" />
      <view class="hero-overlay">
        <text class="hero-project-name">{{ project.projectName }}</text>
        <text class="hero-address">{{ project.address }}</text>
        <view class="hero-stats-row">
          <view class="hero-stat">
            <text class="hero-stat-val">{{ project.availableCount ?? '--' }}</text>
            <text class="hero-stat-label">在售</text>
          </view>
          <view class="hero-stat-divider" />
          <view class="hero-stat">
            <text class="hero-stat-val">{{ project.soldCount ?? '--' }}</text>
            <text class="hero-stat-label">已售</text>
          </view>
          <view class="hero-stat-divider" />
          <view class="hero-stat">
            <text class="hero-stat-val price-text">{{ priceRange }}</text>
            <text class="hero-stat-label">元/m²</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Tab 切换 -->
    <view class="tab-bar">
      <view
        v-for="tab in TABS" :key="tab.key"
        :class="['tab-item', activeTab === tab.key && 'tab-item--active']"
        @click="activeTab = tab.key"
      >
        <text class="tab-text">{{ tab.label }}</text>
      </view>
    </view>

    <!-- Tab 内容区 -->
    <scroll-view scroll-y class="tab-content">

      <!-- ── 户型 Tab ── -->
      <view v-if="activeTab === 'houseType'" class="house-type-tab">
        <view v-if="loadingHT" class="loading-wrap"><u-loading-icon /></view>
        <view v-else-if="houseTypes.length === 0" class="empty-wrap">
          <text class="empty-text">暂无户型数据</text>
        </view>
        <view v-else class="ht-grid">
          <view v-for="ht in houseTypes" :key="ht.id" class="ht-card">
            <image class="ht-image" :src="ht.layoutImage || defaultCover" mode="aspectFill" />
            <view class="ht-info">
              <view class="ht-header">
                <text class="ht-name">{{ ht.name }}</text>
                <text class="ht-code">{{ ht.code }}</text>
              </view>
              <view class="ht-rooms">
                <text class="ht-room-text">{{ ht.bedrooms }}室{{ ht.livingRooms }}厅{{ ht.bathrooms }}卫</text>
                <text class="ht-orientation">{{ ht.orientation }}</text>
              </view>
              <view class="ht-footer">
                <text class="ht-area">{{ ht.area }}m²</text>
                <text class="ht-price">{{ fmtPrice(ht.basePrice) }}元/m²起</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- ── 房源状态 Tab ── -->
      <view v-if="activeTab === 'unitStatus'" class="unit-status-tab">
        <view v-if="loadingMatrix" class="loading-wrap"><u-loading-icon /></view>
        <view v-else-if="matrix.length === 0" class="empty-wrap">
          <text class="empty-text">暂无房源数据</text>
        </view>
        <template v-else>
          <!-- 图例 -->
          <view class="legend-row">
            <view v-for="leg in LEGENDS" :key="leg.status" class="legend-item">
              <view :class="['legend-dot', `status-${leg.status.toLowerCase()}`]" />
              <text class="legend-label">{{ leg.label }}</text>
            </view>
          </view>

          <!-- 楼栋切换 -->
          <scroll-view scroll-x class="building-tabs">
            <view
              v-for="b in matrix" :key="b.building"
              :class="['building-tab', activeBuilding === b.building && 'building-tab--active']"
              @click="activeBuilding = b.building"
            >
              <text class="building-tab-text">{{ b.building }}</text>
            </view>
          </scroll-view>

          <!-- 矩阵（当前楼栋） -->
          <view class="matrix-wrap" v-if="currentBuilding">
            <view v-for="floorRow in currentBuilding.floors" :key="floorRow.floor" class="floor-row">
              <text class="floor-label">{{ floorRow.floor }}F</text>
              <view class="units-row">
                <view
                  v-for="unit in floorRow.units" :key="unit.id"
                  :class="['unit-cell', `status-${unit.status.toLowerCase()}`]"
                  @click="toUnitDetail(unit.id)"
                >
                  <text class="unit-no">{{ unit.unitNo }}</text>
                  <text class="unit-price">{{ unitPriceShort(unit.listPrice) }}</text>
                </view>
              </view>
            </view>
          </view>
        </template>
      </view>

      <!-- ── 销售联系 Tab ── -->
      <view v-if="activeTab === 'contact'" class="contact-tab">
        <view class="contact-card">
          <view class="contact-row">
            <u-icon name="account" size="20" color="#07c160" />
            <text class="contact-label">驻场顾问</text>
            <text class="contact-val">{{ project.contactPerson || '未设置' }}</text>
          </view>
          <view class="contact-row">
            <u-icon name="phone" size="20" color="#07c160" />
            <text class="contact-label">联系电话</text>
            <text class="contact-val contact-phone" @click="callPhone">{{ project.contactPhone || '未设置' }}</text>
          </view>
          <view class="contact-row">
            <u-icon name="calendar" size="20" color="#07c160" />
            <text class="contact-label">代理周期</text>
            <text class="contact-val">{{ project.agencyStartDate }} ~ {{ project.agencyEndDate }}</text>
          </view>
          <view class="contact-desc" v-if="project.description">
            <text class="contact-desc-title">项目介绍</text>
            <text class="contact-desc-content">{{ project.description }}</text>
          </view>
        </view>
      </view>

    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getProject, listHouseTypes, getUnitMatrix } from '@/utils/realtyApi'

const TABS = [
  { key: 'houseType',  label: '户型' },
  { key: 'unitStatus', label: '房源状态' },
  { key: 'contact',    label: '销售联系' }
]
const LEGENDS = [
  { status: 'AVAILABLE',   label: '可售' },
  { status: 'RESERVED',    label: '预留' },
  { status: 'SUBSCRIBED',  label: '认购' },
  { status: 'SIGNED',      label: '签约' },
  { status: 'SOLD',        label: '已售' }
]

const defaultCover = 'https://picsum.photos/seed/default/400/300'

const project       = ref({})
const houseTypes    = ref([])
const matrix        = ref([])
const activeTab     = ref('houseType')
const activeBuilding= ref('')
const loadingHT     = ref(false)
const loadingMatrix = ref(false)

const pages  = getCurrentPages()
const opts   = pages[pages.length - 1]?.options || {}
const projId = Number(opts.id)

const priceRange = computed(() => {
  const p = project.value
  if (!p.minPrice) return '待定'
  const fmt = v => (v / 10000).toFixed(0) + 'w'
  return p.minPrice === p.maxPrice ? `${fmt(p.minPrice)}` : `${fmt(p.minPrice)}-${fmt(p.maxPrice)}`
})

const currentBuilding = computed(() =>
  matrix.value.find(b => b.building === activeBuilding.value)
)

async function loadProject() {
  try {
    const res = await getProject(projId)
    project.value = res.data || {}
    uni.setNavigationBarTitle({ title: project.value.projectName || '楼盘详情' })
  } catch { uni.showToast({ title: '加载失败', icon: 'none' }) }
}

async function loadHouseTypes() {
  loadingHT.value = true
  try {
    const res = await listHouseTypes(projId)
    houseTypes.value = res.data || []
  } finally { loadingHT.value = false }
}

async function loadMatrix() {
  loadingMatrix.value = true
  try {
    const res = await getUnitMatrix(projId)
    matrix.value = res.data || []
    if (matrix.value.length) activeBuilding.value = matrix.value[0].building
  } finally { loadingMatrix.value = false }
}

function fmtPrice(v) { return v ? Number(v).toLocaleString() : '--' }
function unitPriceShort(v) {
  if (!v) return '--'
  return (v / 10000).toFixed(0) + '万'
}

function toUnitDetail(id) { uni.navigateTo({ url: `/pages/realty/unit-detail?id=${id}` }) }
function callPhone() {
  const phone = project.value.contactPhone
  if (phone) uni.makePhoneCall({ phoneNumber: phone })
}

onMounted(async () => {
  await loadProject()
  loadHouseTypes()
  loadMatrix()
})
</script>

<style lang="scss" scoped>
.project-detail {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

// ── 头部
.hero-section { position: relative; height: 220px; }
.hero-image { width: 100%; height: 100%; display: block; }
.hero-overlay {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  background: linear-gradient(transparent, rgba(0,0,0,.7));
  padding: 20px 16px 12px;
}
.hero-project-name { font-size: 20px; font-weight: 700; color: #fff; display: block; }
.hero-address { font-size: 12px; color: rgba(255,255,255,.8); margin-top: 4px; display: block; }
.hero-stats-row {
  display: flex;
  align-items: center;
  margin-top: 10px;
  gap: 0;
}
.hero-stat { display: flex; flex-direction: column; align-items: center; }
.hero-stat-val { font-size: 18px; font-weight: 700; color: #fff; }
.hero-stat-val.price-text { font-size: 15px; color: #ffd54f; }
.hero-stat-label { font-size: 11px; color: rgba(255,255,255,.75); }
.hero-stat-divider { width: 1px; height: 24px; background: rgba(255,255,255,.3); margin: 0 14px; }

// ── Tab 栏
.tab-bar {
  display: flex;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.tab-item {
  flex: 1;
  padding: 12px 0;
  text-align: center;
  &--active { border-bottom: 2px solid #07c160; }
}
.tab-text { font-size: 14px; color: #666; }
.tab-item--active .tab-text { color: #07c160; font-weight: 600; }

.tab-content { flex: 1; }

// ── 通用占位
.loading-wrap, .empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}
.empty-text { font-size: 14px; color: #bbb; margin-top: 8px; }

// ── 户型 Tab
.house-type-tab { padding: 12px; }
.ht-grid { display: flex; flex-direction: column; gap: 12px; }
.ht-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  box-shadow: 0 1px 6px rgba(0,0,0,.06);
}
.ht-image { width: 120px; height: 100px; flex-shrink: 0; }
.ht-info { flex: 1; padding: 10px; display: flex; flex-direction: column; justify-content: space-between; }
.ht-header { display: flex; justify-content: space-between; align-items: center; }
.ht-name { font-size: 15px; font-weight: 600; color: #222; }
.ht-code { font-size: 12px; color: #999; background: #f5f5f5; padding: 1px 6px; border-radius: 8px; }
.ht-rooms { display: flex; justify-content: space-between; }
.ht-room-text { font-size: 12px; color: #555; }
.ht-orientation { font-size: 12px; color: #999; }
.ht-footer { display: flex; justify-content: space-between; align-items: baseline; }
.ht-area { font-size: 13px; color: #333; }
.ht-price { font-size: 13px; color: #e74c3c; font-weight: 500; }

// ── 房源状态 Tab
.unit-status-tab { padding: 12px; }

.legend-row { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 12px; background: #fff; padding: 10px 12px; border-radius: 8px; }
.legend-item { display: flex; align-items: center; gap: 5px; }
.legend-dot { width: 12px; height: 12px; border-radius: 3px; }
.legend-label { font-size: 12px; color: #555; }

.building-tabs { white-space: nowrap; margin-bottom: 10px; }
.building-tab {
  display: inline-block;
  padding: 6px 16px;
  margin-right: 8px;
  border-radius: 16px;
  border: 1px solid #ddd;
  background: #fff;
  &--active { border-color: #07c160; background: #e8faf0; }
}
.building-tab-text { font-size: 13px; color: #555; }
.building-tab--active .building-tab-text { color: #07c160; font-weight: 600; }

.matrix-wrap { background: #fff; border-radius: 10px; padding: 10px; }
.floor-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
.floor-label { width: 30px; font-size: 12px; color: #999; text-align: right; margin-right: 8px; flex-shrink: 0; }
.units-row { display: flex; gap: 6px; flex-wrap: wrap; }
.unit-cell {
  width: 60px;
  height: 44px;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  cursor: pointer;
}
.unit-no   { font-size: 11px; font-weight: 600; color: #fff; }
.unit-price{ font-size: 10px; color: rgba(255,255,255,.85); }

// 状态色
$status-available:  #27ae60;
$status-reserved:   #f39c12;
$status-subscribed: #2980b9;
$status-signed:     #8e44ad;
$status-sold:       #95a5a6;
$status-unavailable:#e74c3c;

.status-available   { background: $status-available;  }
.status-reserved    { background: $status-reserved;   }
.status-subscribed  { background: $status-subscribed; }
.status-signed      { background: $status-signed;     }
.status-sold        { background: $status-sold;       }
.status-unavailable { background: $status-unavailable;}

// 图例点（同色）
.legend-dot.status-available   { background: $status-available;  }
.legend-dot.status-reserved    { background: $status-reserved;   }
.legend-dot.status-subscribed  { background: $status-subscribed; }
.legend-dot.status-signed      { background: $status-signed;     }
.legend-dot.status-sold        { background: $status-sold;       }

// ── 销售联系 Tab
.contact-tab { padding: 12px; }
.contact-card { background: #fff; border-radius: 10px; padding: 16px; }
.contact-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  &:last-of-type { border-bottom: none; }
}
.contact-label { font-size: 13px; color: #999; width: 60px; flex-shrink: 0; }
.contact-val { font-size: 14px; color: #333; flex: 1; }
.contact-phone { color: #007aff; }
.contact-desc { margin-top: 16px; padding-top: 12px; border-top: 1px solid #f5f5f5; }
.contact-desc-title { font-size: 13px; color: #999; margin-bottom: 8px; display: block; }
.contact-desc-content { font-size: 14px; color: #555; line-height: 1.6; }
</style>
