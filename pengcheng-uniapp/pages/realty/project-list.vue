<template>
  <view class="project-list">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-input-wrap">
        <u-icon name="search" color="#999" size="16" />
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索楼盘名称/地址"
          placeholder-class="search-placeholder"
          @confirm="onSearch"
          confirm-type="search"
        />
        <u-icon v-if="keyword" name="close-circle-fill" color="#ccc" size="16" @click="clearKeyword" />
      </view>
      <text class="filter-btn" @click="showFilter = true">筛选</text>
    </view>

    <!-- 筛选条（已选中条件标签） -->
    <view v-if="activeFilters.length" class="filter-tags">
      <view v-for="tag in activeFilters" :key="tag.key" class="filter-tag">
        <text class="filter-tag-text">{{ tag.label }}</text>
        <u-icon name="close" size="10" color="#007aff" @click="removeFilter(tag.key)" />
      </view>
    </view>

    <!-- 列表主体 -->
    <scroll-view scroll-y class="list-scroll" @scrolltolower="loadMore" refresher-enabled
      :refresher-triggered="refreshing" @refresherrefresh="onRefresh">

      <view v-if="loading && list.length === 0" class="loading-wrap">
        <u-loading-icon />
        <text class="loading-text">加载中...</text>
      </view>

      <view v-else-if="list.length === 0" class="empty-wrap">
        <u-icon name="file-text" size="48" color="#ddd" />
        <text class="empty-text">暂无楼盘数据</text>
      </view>

      <template v-else>
        <view v-for="project in list" :key="project.id" class="project-card"
          @click="toDetail(project.id)">
          <!-- 封面图 -->
          <image class="project-cover" :src="project.coverImage || defaultCover" mode="aspectFill" />
          <!-- 状态角标 -->
          <view :class="['status-badge', statusBadgeClass(project.status)]">
            <text class="status-badge-text">{{ statusLabel(project.status) }}</text>
          </view>

          <!-- 信息区 -->
          <view class="project-info">
            <text class="project-name">{{ project.projectName }}</text>
            <text class="project-address">{{ project.address }}</text>

            <!-- 统计行 -->
            <view class="project-stats">
              <view class="stat-item">
                <text class="stat-num available">{{ project.availableCount }}</text>
                <text class="stat-label">在售</text>
              </view>
              <view class="stat-divider" />
              <view class="stat-item">
                <text class="stat-num sold">{{ project.soldCount }}</text>
                <text class="stat-label">已售</text>
              </view>
            </view>

            <!-- 价格行 -->
            <view class="project-price-row">
              <text class="price-from">{{ priceRange(project) }}</text>
              <view class="arrow-wrap">
                <u-icon name="arrow-right" size="14" color="#999" />
              </view>
            </view>
          </view>
        </view>

        <!-- 底部提示 -->
        <view class="list-bottom">
          <text v-if="noMore" class="no-more-text">已加载全部</text>
          <u-loading-icon v-else-if="loadingMore" />
        </view>
      </template>
    </scroll-view>

    <!-- 筛选弹窗 -->
    <u-popup v-model:show="showFilter" mode="bottom" border-radius="16">
      <view class="filter-popup">
        <view class="filter-popup-header">
          <text class="filter-popup-title">筛选条件</text>
          <u-icon name="close" size="18" @click="showFilter = false" />
        </view>

        <view class="filter-section">
          <text class="filter-section-title">区域</text>
          <view class="filter-options">
            <view
              v-for="r in regionOptions" :key="r"
              :class="['filter-option', filterForm.region === r && 'filter-option--active']"
              @click="toggleRegion(r)"
            >
              <text class="filter-option-text">{{ r }}</text>
            </view>
          </view>
        </view>

        <view class="filter-section">
          <text class="filter-section-title">价位（万元/m²）</text>
          <view class="filter-options">
            <view
              v-for="p in priceOptions" :key="p.label"
              :class="['filter-option', filterForm.priceKey === p.label && 'filter-option--active']"
              @click="selectPrice(p)"
            >
              <text class="filter-option-text">{{ p.label }}</text>
            </view>
          </view>
        </view>

        <view class="filter-actions">
          <u-button text="重置" plain @click="resetFilter" />
          <u-button text="确定" type="primary" @click="applyFilter" />
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { listProjects } from '@/utils/realtyApi'

const keyword    = ref('')
const showFilter = ref(false)
const list       = ref([])
const loading    = ref(false)
const refreshing = ref(false)
const loadingMore= ref(false)
const noMore     = ref(false)
const page       = ref(1)
const PAGE_SIZE  = 10

const defaultCover = 'https://picsum.photos/seed/default/400/300'

const regionOptions = ['望京', '中关村', '丽泽', '通州', '亦庄', '大兴']
const priceOptions  = [
  { label: '4万以下',  min: 0,     max: 40000 },
  { label: '4-6万',    min: 40000, max: 60000 },
  { label: '6-8万',    min: 60000, max: 80000 },
  { label: '8万以上',  min: 80000, max: null  }
]

const filterForm = reactive({ region: '', priceKey: '', priceMin: null, priceMax: null })

const activeFilters = computed(() => {
  const tags = []
  if (filterForm.region)   tags.push({ key: 'region',   label: filterForm.region })
  if (filterForm.priceKey) tags.push({ key: 'price',    label: filterForm.priceKey })
  return tags
})

function removeFilter(key) {
  if (key === 'region') { filterForm.region = '' }
  if (key === 'price')  { filterForm.priceKey = ''; filterForm.priceMin = null; filterForm.priceMax = null }
  fetchList(true)
}

function toggleRegion(r) { filterForm.region = filterForm.region === r ? '' : r }
function selectPrice(p)  {
  filterForm.priceKey = filterForm.priceKey === p.label ? '' : p.label
  filterForm.priceMin = filterForm.priceKey ? p.min : null
  filterForm.priceMax = filterForm.priceKey ? p.max : null
}
function resetFilter() {
  filterForm.region = ''; filterForm.priceKey = ''; filterForm.priceMin = null; filterForm.priceMax = null
}
function applyFilter() { showFilter.value = false; fetchList(true) }

function clearKeyword() { keyword.value = ''; fetchList(true) }
function onSearch()     { fetchList(true) }
function onRefresh()    { refreshing.value = true; fetchList(true) }
function loadMore()     { if (!noMore.value && !loadingMore.value) fetchList(false) }

async function fetchList(reset = false) {
  if (reset) { page.value = 1; noMore.value = false }
  if (page.value === 1) loading.value = true
  else loadingMore.value = true

  try {
    const res = await listProjects({
      page: page.value,
      pageSize: PAGE_SIZE,
      keyword: keyword.value || undefined,
      region:  filterForm.region || undefined,
      priceMin: filterForm.priceMin,
      priceMax: filterForm.priceMax
    })
    const newList = res.data?.list || []
    list.value = reset ? newList : [...list.value, ...newList]
    if (newList.length < PAGE_SIZE) noMore.value = true
    else page.value++
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
    loadingMore.value = false
    refreshing.value = false
  }
}

function toDetail(id) {
  uni.navigateTo({ url: `/pages/realty/project-detail?id=${id}` })
}

function statusLabel(status) {
  return { 1: '在售', 2: '待售', 3: '售罄', 4: '已到期' }[status] || '未知'
}
function statusBadgeClass(status) {
  return { 1: 'badge-onsale', 2: 'badge-pending', 3: 'badge-sold', 4: 'badge-expired' }[status] || ''
}
function priceRange(project) {
  if (!project.minPrice) return '价格待定'
  const fmt = v => (v / 10000).toFixed(0) + '万'
  return project.minPrice === project.maxPrice
    ? `${fmt(project.minPrice)}/m²`
    : `${fmt(project.minPrice)}-${fmt(project.maxPrice)}元/m²`
}

onMounted(() => fetchList(true))
</script>

<style lang="scss" scoped>
.project-list {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

// ── 搜索栏
.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f5f5f5;
  border-radius: 20px;
  padding: 6px 12px;
}
.search-input { flex: 1; font-size: 14px; color: #333; }
.search-placeholder { color: #bbb; }
.filter-btn { font-size: 14px; color: #007aff; white-space: nowrap; }

// ── 筛选标签
.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}
.filter-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #e8f0ff;
  border-radius: 12px;
  padding: 3px 8px;
}
.filter-tag-text { font-size: 12px; color: #007aff; }

// ── 列表滚动区
.list-scroll { flex: 1; }

.loading-wrap, .empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 12px;
}
.loading-text, .empty-text { font-size: 14px; color: #999; }

// ── 楼盘卡片
.project-card {
  margin: 12px 12px 0;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, .06);
  position: relative;
}
.project-cover {
  width: 100%;
  height: 180px;
  display: block;
}

.status-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 8px;
  border-radius: 10px;
  &.badge-onsale  { background: rgba(7, 193, 96, .85); }
  &.badge-pending { background: rgba(255, 152, 0, .85); }
  &.badge-sold    { background: rgba(153, 153, 153, .85); }
  &.badge-expired { background: rgba(200, 50, 50, .85); }
}
.status-badge-text { font-size: 11px; color: #fff; }

.project-info { padding: 12px; }
.project-name { font-size: 16px; font-weight: 600; color: #222; display: block; }
.project-address { font-size: 12px; color: #999; margin-top: 4px; display: block; }

.project-stats {
  display: flex;
  align-items: center;
  margin-top: 10px;
}
.stat-item {
  display: flex;
  align-items: baseline;
  gap: 3px;
}
.stat-num {
  font-size: 18px;
  font-weight: 600;
  &.available { color: #07c160; }
  &.sold      { color: #999; }
}
.stat-label { font-size: 12px; color: #999; }
.stat-divider { width: 1px; height: 14px; background: #eee; margin: 0 12px; }

.project-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f5f5f5;
}
.price-from { font-size: 13px; color: #e74c3c; font-weight: 500; }

.list-bottom { text-align: center; padding: 16px 0; }
.no-more-text { font-size: 12px; color: #ccc; }

// ── 筛选弹窗
.filter-popup { padding: 16px; }
.filter-popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.filter-popup-title { font-size: 16px; font-weight: 600; }
.filter-section { margin-bottom: 16px; }
.filter-section-title { font-size: 13px; color: #666; margin-bottom: 10px; display: block; }
.filter-options { display: flex; flex-wrap: wrap; gap: 8px; }
.filter-option {
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid #ddd;
  background: #fafafa;
  &--active { border-color: #007aff; background: #e8f0ff; }
}
.filter-option-text { font-size: 13px; color: #333; }
.filter-option--active .filter-option-text { color: #007aff; }
.filter-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
</style>
