<template>
  <div class="market-page">
    <!-- 顶部栏 -->
    <div class="market-header">
      <div class="header-left">
        <h2 class="market-title">模板市场</h2>
        <p class="market-subtitle">发现并使用高质量行业模板，快速启动你的表格</p>
      </div>
      <div class="header-right">
        <n-button type="primary" ghost @click="openShareModal">
          <template #icon><n-icon><ShareSocialOutline /></n-icon></template>
          分享我的模板
        </n-button>
      </div>
    </div>

    <!-- 分类 Tab + 搜索 + 排序 -->
    <div class="market-toolbar">
      <n-tabs
        v-model:value="activeCategory"
        type="segment"
        size="small"
        @update:value="handleCategoryChange"
      >
        <n-tab name="">全部</n-tab>
        <n-tab name="general">通用</n-tab>
        <n-tab name="realty">房产</n-tab>
        <n-tab name="sales">销售</n-tab>
        <n-tab name="hr">人事</n-tab>
        <n-tab name="finance">财务</n-tab>
      </n-tabs>

      <div class="toolbar-right">
        <n-input
          v-model:value="keyword"
          placeholder="搜索模板名称..."
          clearable
          size="small"
          style="width: 200px"
          @keyup.enter="doSearch"
          @clear="doSearch"
        >
          <template #prefix><n-icon><SearchOutline /></n-icon></template>
        </n-input>

        <n-select
          v-model:value="sort"
          size="small"
          style="width: 120px"
          :options="sortOptions"
          @update:value="doSearch"
        />
      </div>
    </div>

    <!-- 卡片网格 -->
    <div class="market-body" v-loading="loading">
      <n-spin :show="loading">
        <n-empty
          v-if="!loading && templates.length === 0"
          description="暂无符合条件的模板"
          style="margin-top: 60px"
        />

        <div class="cards-grid" v-else>
          <div
            v-for="tpl in templates"
            :key="tpl.id"
            class="market-card"
            @click="openDetail(tpl)"
          >
            <div class="card-icon">{{ tpl.icon || '📊' }}</div>
            <div class="card-body">
              <div class="card-name">{{ tpl.name }}</div>
              <div class="card-desc">{{ tpl.description }}</div>
              <div class="card-author" v-if="tpl.authorName">
                <n-icon size="12"><PersonOutline /></n-icon>
                {{ tpl.authorName }}
              </div>
              <div class="card-tags" v-if="tpl.tags">
                <n-tag
                  v-for="tag in tpl.tags.split(',').slice(0, 3)"
                  :key="tag"
                  size="tiny"
                  round
                  style="margin-right: 4px"
                >{{ tag.trim() }}</n-tag>
              </div>
            </div>
            <div class="card-footer">
              <div class="card-stat">
                <n-icon size="13"><DownloadOutline /></n-icon>
                <span>{{ tpl.downloadCount || 0 }}</span>
              </div>
              <div class="card-rating">
                <n-rate
                  :value="Number(tpl.avgRating || 0)"
                  :count="5"
                  readonly
                  size="small"
                  allow-half
                />
                <span class="rating-num">{{ formatRating(tpl.avgRating) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="market-pagination" v-if="total > pageSize">
          <n-pagination
            v-model:page="page"
            :page-size="pageSize"
            :item-count="total"
            show-quick-jumper
            @update:page="loadMarket"
          />
        </div>
      </n-spin>
    </div>

    <!-- 详情抽屉 -->
    <n-drawer v-model:show="showDetail" :width="520" placement="right">
      <n-drawer-content :native-scrollbar="false" v-if="detail">
        <template #header>
          <div class="detail-header">
            <span class="detail-icon">{{ detail.icon || '📊' }}</span>
            <div class="detail-title-wrap">
              <div class="detail-name">{{ detail.name }}</div>
              <n-tag size="small" type="info">{{ categoryLabel(detail.category) }}</n-tag>
            </div>
          </div>
        </template>

        <div class="detail-section">
          <p class="detail-desc">{{ detail.description || '暂无描述' }}</p>
        </div>

        <n-divider>基本信息</n-divider>
        <n-descriptions :column="2" label-placement="left" bordered size="small">
          <n-descriptions-item label="作者">{{ detail.authorName || '官方' }}</n-descriptions-item>
          <n-descriptions-item label="下载量">{{ detail.downloadCount || 0 }}</n-descriptions-item>
          <n-descriptions-item label="评分">
            <div style="display: flex; align-items: center; gap: 6px">
              <n-rate :value="Number(detail.avgRating || 0)" :count="5" readonly size="small" allow-half />
              <span>{{ formatRating(detail.avgRating) }}（{{ detail.ratingCount || 0 }} 人评分）</span>
            </div>
          </n-descriptions-item>
          <n-descriptions-item label="分类">{{ categoryLabel(detail.category) }}</n-descriptions-item>
        </n-descriptions>

        <n-divider>字段预览</n-divider>
        <div class="field-preview" v-if="detail.fieldsConfig?.length">
          <n-tag
            v-for="f in detail.fieldsConfig"
            :key="f.field_key"
            size="small"
            style="margin-right: 6px; margin-bottom: 6px"
          >
            {{ f.name }}
            <template #icon>
              <n-icon size="12"><DocumentTextOutline /></n-icon>
            </template>
          </n-tag>
        </div>
        <n-empty v-else description="暂无字段信息" size="small" />

        <n-divider v-if="detail.tags">标签</n-divider>
        <div v-if="detail.tags" class="detail-tags">
          <n-tag
            v-for="tag in detail.tags.split(',')"
            :key="tag"
            size="small"
            round
            style="margin-right: 6px; margin-bottom: 6px"
          >{{ tag.trim() }}</n-tag>
        </div>

        <template #footer>
          <n-space>
            <n-button
              type="primary"
              :loading="downloading"
              @click="handleDownload"
            >
              <template #icon><n-icon><DownloadOutline /></n-icon></template>
              使用此模板
            </n-button>
            <n-button @click="openRating">
              <template #icon><n-icon><StarOutline /></n-icon></template>
              评分
            </n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 评分对话框 -->
    <RatingDialog
      v-model:show="showRating"
      :template-id="detail?.id ?? null"
      @rated="handleRated"
    />

    <!-- 分享我的模板弹窗 -->
    <n-modal v-model:show="showShare" preset="card" title="分享模板到市场" style="width: 520px">
      <n-form :model="shareForm" label-placement="left" label-width="80">
        <n-form-item label="选择模板" required>
          <n-select
            v-model:value="shareForm.templateId"
            :options="privateTemplateOptions"
            :loading="loadingPrivate"
            placeholder="选择要分享的私有模板"
            @update:value="onPrivateTemplateSelect"
          />
        </n-form-item>
        <n-form-item label="作者署名">
          <n-input v-model:value="shareForm.authorName" placeholder="留空则匿名" />
        </n-form-item>
        <n-form-item label="标签">
          <n-input
            v-model:value="shareForm.tags"
            placeholder="逗号分隔，如：通用,日常,工作"
          />
        </n-form-item>
      </n-form>
      <n-alert type="info" size="small" style="margin-top: 8px">
        提交后将进入审核流程，管理员审核通过后公开显示。
      </n-alert>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showShare = false">取消</n-button>
          <n-button
            type="primary"
            :loading="sharing"
            :disabled="!shareForm.templateId"
            @click="handleShare"
          >
            提交分享
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  NButton, NIcon, NInput, NSelect, NTabs, NTab, NCard, NTag, NRate,
  NDrawer, NDrawerContent, NModal, NForm, NFormItem, NEmpty, NSpin,
  NPagination, NSpace, NDivider, NDescriptions, NDescriptionsItem, NAlert,
  useMessage
} from 'naive-ui'
import {
  SearchOutline, ShareSocialOutline, DownloadOutline, StarOutline,
  PersonOutline, DocumentTextOutline
} from '@vicons/ionicons5'
import { smartTableMarketApi, type MarketTemplate } from '@/api/smartTableMarket'
import { smartTableApi } from '@/api/smartTable'
import RatingDialog from './components/RatingDialog.vue'

const message = useMessage()

// ==================== 状态 ====================
const templates = ref<MarketTemplate[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

const activeCategory = ref('')
const keyword = ref('')
const sort = ref<'downloads' | 'rating' | 'latest'>('downloads')

const sortOptions = [
  { label: '按下载量', value: 'downloads' },
  { label: '按评分', value: 'rating' },
  { label: '最新发布', value: 'latest' }
]

const categoryMap: Record<string, string> = {
  general: '通用', realty: '房产', sales: '销售', hr: '人事', finance: '财务'
}
function categoryLabel(cat: string) { return categoryMap[cat] || cat }
function formatRating(val?: number) {
  if (!val) return '暂无'
  return Number(val).toFixed(1)
}

// ==================== 详情抽屉 ====================
const showDetail = ref(false)
const detail = ref<MarketTemplate | null>(null)
const downloading = ref(false)
const showRating = ref(false)

function openDetail(tpl: MarketTemplate) {
  detail.value = tpl
  showDetail.value = true
}

async function handleDownload() {
  if (!detail.value) return
  downloading.value = true
  try {
    await smartTableMarketApi.downloadTemplate(detail.value.id)
    message.success('下载成功，已记录使用次数')
    detail.value.downloadCount = (detail.value.downloadCount || 0) + 1
  } catch {
    // request 拦截器已弹错
  } finally {
    downloading.value = false
  }
}

function openRating() {
  showRating.value = true
}

function handleRated() {
  if (detail.value) {
    loadMarket()
  }
}

// ==================== 加载数据 ====================
async function loadMarket() {
  loading.value = true
  try {
    const res: any = await smartTableMarketApi.listMarket({
      category: activeCategory.value || undefined,
      keyword: keyword.value || undefined,
      sort: sort.value,
      page: page.value,
      size: pageSize.value
    })
    templates.value = res?.records || res?.list || (Array.isArray(res) ? res : [])
    total.value = res?.total || 0
  } catch {
    // request 拦截器已弹错
  } finally {
    loading.value = false
  }
}

function handleCategoryChange() {
  page.value = 1
  loadMarket()
}

function doSearch() {
  page.value = 1
  loadMarket()
}

// ==================== 分享 ====================
const showShare = ref(false)
const sharing = ref(false)
const loadingPrivate = ref(false)
const privateTemplateOptions = ref<{ label: string; value: number }[]>([])
const shareForm = ref({ templateId: null as number | null, authorName: '', tags: '' })

async function openShareModal() {
  showShare.value = true
  shareForm.value = { templateId: null, authorName: '', tags: '' }
  loadingPrivate.value = true
  try {
    const res: any = await smartTableApi.listTemplates()
    const list = (res as any)?.data || (Array.isArray(res) ? res : [])
    privateTemplateOptions.value = list
      .filter((t: any) => !t.builtIn)
      .map((t: any) => ({ label: `${t.icon || '📊'} ${t.name}`, value: t.id }))
  } catch {
    // ignore
  } finally {
    loadingPrivate.value = false
  }
}

function onPrivateTemplateSelect() {
  // 预填充作者署名可以从 userStore.user.nickname 获取，这里保持空白由用户自填
}

async function handleShare() {
  if (!shareForm.value.templateId) return
  sharing.value = true
  try {
    await smartTableMarketApi.shareTemplate(shareForm.value.templateId, {
      authorName: shareForm.value.authorName || '',
      tags: shareForm.value.tags || undefined
    })
    message.success('分享申请已提交，等待管理员审核')
    showShare.value = false
  } catch {
    // request 拦截器已弹错
  } finally {
    sharing.value = false
  }
}

// ==================== 生命周期 ====================
onMounted(loadMarket)
</script>

<style scoped>
.market-page {
  padding: 20px 24px;
  min-height: calc(100vh - 100px);
  background: #f9f9f9;
}

/* 顶部标题区 */
.market-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.market-title {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}
.market-subtitle {
  margin: 0;
  font-size: 13px;
  color: #999;
}

/* 工具栏 */
.market-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 卡片网格 — 响应式：
   ≥1440px  5列  |  ≥1200px  4列  |  ≥900px  3列  |  ≥600px  2列  |  <600px  1列 */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

@media (max-width: 1439px) {
  .cards-grid { grid-template-columns: repeat(4, 1fr); }
}
@media (max-width: 1199px) {
  .cards-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 899px) {
  .cards-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 599px) {
  .cards-grid { grid-template-columns: 1fr; }
}

/* 市场卡片 */
.market-card {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e8e8e8;
  padding: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s, transform 0.15s;
  display: flex;
  flex-direction: column;
}
.market-card:hover {
  box-shadow: 0 4px 16px rgba(24, 160, 88, 0.14);
  border-color: #18a058;
  transform: translateY(-2px);
}
.card-icon {
  font-size: 36px;
  margin-bottom: 10px;
}
.card-body {
  flex: 1;
}
.card-name {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 6px;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-desc {
  font-size: 12px;
  color: #888;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 32px;
}
.card-author {
  font-size: 11px;
  color: #aaa;
  display: flex;
  align-items: center;
  gap: 3px;
  margin-bottom: 6px;
}
.card-tags {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f0f0f0;
  padding-top: 8px;
  margin-top: auto;
}
.card-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #aaa;
}
.card-rating {
  display: flex;
  align-items: center;
  gap: 4px;
}
.rating-num {
  font-size: 11px;
  color: #f0a020;
}

/* 分页 */
.market-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

/* 详情抽屉 */
.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.detail-icon {
  font-size: 32px;
}
.detail-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-name {
  font-weight: 700;
  font-size: 16px;
}
.detail-section {
  margin-bottom: 4px;
}
.detail-desc {
  color: #555;
  font-size: 14px;
  line-height: 1.7;
  margin: 0;
}
.field-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.detail-tags {
  display: flex;
  flex-wrap: wrap;
}
</style>
