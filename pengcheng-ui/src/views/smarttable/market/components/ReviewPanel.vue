<template>
  <div class="review-panel">
    <n-card title="模板审核" v-if="isAdmin">
      <template #header-extra>
        <n-button size="small" @click="loadReviewList" :loading="loading">刷新</n-button>
      </template>

      <n-empty v-if="!loading && list.length === 0" description="暂无待审核模板" />

      <div v-else class="review-list">
        <n-card
          v-for="tpl in list"
          :key="tpl.id"
          size="small"
          class="review-item"
        >
          <div class="item-header">
            <span class="item-icon">{{ tpl.icon || '📊' }}</span>
            <div class="item-info">
              <div class="item-name">{{ tpl.name }}</div>
              <div class="item-meta">
                <n-tag size="tiny" type="warning">审核中</n-tag>
                <span class="item-author">{{ tpl.authorName || '匿名' }}</span>
                <span class="item-category">{{ categoryLabel(tpl.category) }}</span>
              </div>
            </div>
          </div>
          <div class="item-desc">{{ tpl.description }}</div>
          <div class="item-tags" v-if="tpl.tags">
            <n-tag
              v-for="tag in tpl.tags.split(',')"
              :key="tag"
              size="tiny"
              round
              style="margin-right: 4px"
            >{{ tag.trim() }}</n-tag>
          </div>
          <div class="item-fields" v-if="tpl.fieldsConfig?.length">
            <span class="fields-label">字段：</span>
            <n-tag
              v-for="f in tpl.fieldsConfig.slice(0, 5)"
              :key="f.field_key"
              size="tiny"
              type="info"
              style="margin-right: 4px"
            >{{ f.name }}</n-tag>
            <n-tag v-if="tpl.fieldsConfig.length > 5" size="tiny">+{{ tpl.fieldsConfig.length - 5 }}</n-tag>
          </div>
          <template #footer>
            <n-space justify="end">
              <n-button
                size="small"
                type="error"
                ghost
                :loading="actionId === tpl.id && actionType === 'reject'"
                @click="handleReview(tpl.id, false)"
              >
                拒绝
              </n-button>
              <n-button
                size="small"
                type="primary"
                :loading="actionId === tpl.id && actionType === 'approve'"
                @click="handleReview(tpl.id, true)"
              >
                通过
              </n-button>
            </n-space>
          </template>
        </n-card>
      </div>

      <n-spin v-if="loading" :show="loading" style="margin: 40px auto; display: block; text-align: center" />
    </n-card>

    <n-result
      v-else
      status="403"
      title="无访问权限"
      description="仅管理员可访问审核面板"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { NCard, NButton, NSpace, NTag, NEmpty, NSpin, NResult, useMessage } from 'naive-ui'
import { smartTableMarketApi, type MarketTemplate } from '@/api/smartTableMarket'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.hasRole('admin'))

const list = ref<MarketTemplate[]>([])
const loading = ref(false)
const actionId = ref<number | null>(null)
const actionType = ref<'approve' | 'reject' | null>(null)

const categoryMap: Record<string, string> = {
  general: '通用',
  realty: '房产',
  sales: '销售',
  hr: '人事',
  finance: '财务'
}

function categoryLabel(cat: string) {
  return categoryMap[cat] || cat
}

async function loadReviewList() {
  if (!isAdmin.value) return
  loading.value = true
  try {
    // 复用 listMarket 并在前端筛选 REVIEWING 状态（后端仅返回 PUBLIC，
    // 管理员可通过此面板浏览时需更宽泛的接口；此处用 page=1&size=100 兜底）
    const res: any = await smartTableMarketApi.listMarket({ sort: 'latest', size: 100 })
    const records: MarketTemplate[] = res?.records || res?.list || (Array.isArray(res) ? res : [])
    list.value = records.filter(t => t.shareStatus === 'REVIEWING')
  } catch {
    // request 拦截器已弹错
  } finally {
    loading.value = false
  }
}

async function handleReview(id: number, approve: boolean) {
  actionId.value = id
  actionType.value = approve ? 'approve' : 'reject'
  try {
    await smartTableMarketApi.reviewTemplate(id, approve)
    message.success(approve ? '已通过该模板' : '已拒绝该模板')
    list.value = list.value.filter(t => t.id !== id)
  } catch {
    // request 拦截器已弹错
  } finally {
    actionId.value = null
    actionType.value = null
  }
}

onMounted(() => {
  if (isAdmin.value) loadReviewList()
})
</script>

<style scoped>
.review-panel {
  padding: 4px;
}
.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.review-item {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  transition: box-shadow 0.2s;
}
.review-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.item-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}
.item-icon {
  font-size: 28px;
  flex-shrink: 0;
}
.item-info {
  flex: 1;
}
.item-name {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 4px;
}
.item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #999;
}
.item-author {
  color: #666;
}
.item-desc {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.item-tags {
  margin-bottom: 6px;
}
.item-fields {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 12px;
}
.fields-label {
  color: #999;
  margin-right: 4px;
}
</style>
