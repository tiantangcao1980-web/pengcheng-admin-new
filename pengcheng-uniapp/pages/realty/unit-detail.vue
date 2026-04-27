<template>
  <view class="unit-detail">
    <!-- 大图 -->
    <view class="unit-hero">
      <image class="unit-hero-image" :src="layoutImage || defaultCover" mode="aspectFill" />
      <!-- 状态角标 -->
      <view :class="['status-badge', `status-${unit.status?.toLowerCase()}`]">
        <text class="status-badge-text">{{ statusLabel }}</text>
      </view>
    </view>

    <!-- 价格条 -->
    <view class="price-bar">
      <view class="price-main">
        <text class="price-total">{{ totalPrice }}</text>
        <text class="price-unit">元</text>
      </view>
      <text class="price-per">{{ unitPrice }}元/m²</text>
    </view>

    <!-- 基本信息卡 -->
    <view class="info-card">
      <text class="info-card-title">房源信息</text>
      <view class="info-grid">
        <view class="info-item">
          <text class="info-label">房源编号</text>
          <text class="info-val">{{ unit.fullNo || '--' }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">楼栋</text>
          <text class="info-val">{{ unit.building || '--' }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">楼层</text>
          <text class="info-val">{{ unit.floor ? unit.floor + '层' : '--' }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">房号</text>
          <text class="info-val">{{ unit.unitNo || '--' }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">建筑面积</text>
          <text class="info-val">{{ unit.area ? unit.area + 'm²' : '--' }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">状态</text>
          <text :class="['info-val', `status-text-${unit.status?.toLowerCase()}`]">{{ statusLabel }}</text>
        </view>
      </view>
    </view>

    <!-- 户型信息卡 -->
    <view class="info-card" v-if="houseType.id">
      <text class="info-card-title">户型信息</text>
      <view class="info-grid">
        <view class="info-item">
          <text class="info-label">户型</text>
          <text class="info-val">{{ houseType.name || '--' }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">户型代码</text>
          <text class="info-val">{{ houseType.code || '--' }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">室/厅/卫</text>
          <text class="info-val">{{ roomDesc }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">朝向</text>
          <text class="info-val">{{ houseType.orientation || '--' }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">套内面积</text>
          <text class="info-val">{{ houseType.insideArea ? houseType.insideArea + 'm²' : '--' }}</text>
        </view>
      </view>
      <!-- 户型图 -->
      <view v-if="houseType.layoutImage" class="ht-image-wrap">
        <image class="ht-image" :src="houseType.layoutImage" mode="widthFix" />
      </view>
    </view>

    <!-- 备注 -->
    <view class="info-card" v-if="unit.remark">
      <text class="info-card-title">备注</text>
      <text class="remark-text">{{ unit.remark }}</text>
    </view>

    <!-- 底部操作 -->
    <view class="bottom-actions">
      <view class="action-contact" @click="callProject">
        <u-icon name="phone" size="20" color="#07c160" />
        <text class="action-contact-text">联系顾问</text>
      </view>
      <u-button class="action-report-btn" type="primary" @click="goReport">登记客户</u-button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUnit, listHouseTypes, getProject } from '@/utils/realtyApi'

const defaultCover = 'https://picsum.photos/seed/default/400/300'

const unit      = ref({})
const houseType = ref({})
const project   = ref({})

const pages = getCurrentPages()
const opts  = pages[pages.length - 1]?.options || {}
const unitId = Number(opts.id)

const STATUS_MAP = {
  AVAILABLE:   '可售',
  RESERVED:    '预留中',
  SUBSCRIBED:  '已认购',
  SIGNED:      '已签约',
  SOLD:        '已售',
  UNAVAILABLE: '暂停销售'
}
const statusLabel = computed(() => STATUS_MAP[unit.value.status] || unit.value.status || '--')

const totalPrice = computed(() => {
  const p = unit.value.listPrice
  if (!p) return '--'
  return (p / 10000).toFixed(0) + '万'
})

const unitPrice = computed(() => {
  const lp = unit.value.listPrice
  const area = unit.value.area
  if (!lp || !area) return '--'
  return Math.round(lp / area).toLocaleString()
})

const layoutImage = computed(() => houseType.value.layoutImage || '')

const roomDesc = computed(() => {
  const ht = houseType.value
  if (!ht.id) return '--'
  return `${ht.bedrooms ?? '--'}室${ht.livingRooms ?? '--'}厅${ht.bathrooms ?? '--'}卫`
})

async function loadUnit() {
  try {
    const res = await getUnit(unitId)
    unit.value = res.data || {}
    uni.setNavigationBarTitle({ title: `${unit.value.fullNo || '房源'} 详情` })

    // 加载关联户型
    if (unit.value.projectId) {
      const htRes = await listHouseTypes(unit.value.projectId)
      const types = htRes.data || []
      houseType.value = types.find(h => h.id === unit.value.houseTypeId) || {}

      // 加载关联楼盘联系信息
      const pRes = await getProject(unit.value.projectId)
      project.value = pRes.data || {}
    }
  } catch { uni.showToast({ title: '房源加载失败', icon: 'none' }) }
}

function callProject() {
  const phone = project.value.contactPhone
  if (phone) uni.makePhoneCall({ phoneNumber: phone })
  else uni.showToast({ title: '暂无联系电话', icon: 'none' })
}

function goReport() {
  uni.navigateTo({ url: `/pages/customer/report?unitId=${unitId}&projectId=${unit.value.projectId}` })
}

onMounted(loadUnit)
</script>

<style lang="scss" scoped>
.unit-detail {
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 80px;
}

// ── 大图
.unit-hero { position: relative; height: 260px; }
.unit-hero-image { width: 100%; height: 100%; display: block; }
.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 12px;
}
.status-badge-text { font-size: 12px; color: #fff; font-weight: 600; }

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

// 状态文字色（info 格内）
.status-text-available   { color: $status-available;  }
.status-text-reserved    { color: $status-reserved;   }
.status-text-subscribed  { color: $status-subscribed; }
.status-text-signed      { color: $status-signed;     }
.status-text-sold        { color: $status-sold;       }
.status-text-unavailable { color: $status-unavailable;}

// ── 价格条
.price-bar {
  background: #fff;
  padding: 14px 16px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
}
.price-main { display: flex; align-items: baseline; gap: 3px; }
.price-total { font-size: 26px; font-weight: 700; color: #e74c3c; }
.price-unit  { font-size: 13px; color: #e74c3c; }
.price-per   { font-size: 13px; color: #999; }

// ── 信息卡
.info-card {
  background: #fff;
  margin: 10px 12px 0;
  border-radius: 10px;
  padding: 14px;
  box-shadow: 0 1px 4px rgba(0,0,0,.05);
}
.info-card-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  display: block;
  padding-bottom: 8px;
  border-bottom: 1px solid #f5f5f5;
}
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 8px;
}
.info-item { display: flex; flex-direction: column; gap: 3px; }
.info-label { font-size: 12px; color: #999; }
.info-val   { font-size: 14px; color: #333; }

.ht-image-wrap { margin-top: 12px; background: #fafafa; border-radius: 8px; overflow: hidden; }
.ht-image { width: 100%; }

.remark-text { font-size: 14px; color: #666; line-height: 1.6; }

// ── 底部操作
.bottom-actions {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  height: 70px;
  background: #fff;
  border-top: 1px solid #eee;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
  z-index: 100;
}
.action-contact {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 0 8px;
}
.action-contact-text { font-size: 10px; color: #07c160; }
.action-report-btn { flex: 1; }
</style>
