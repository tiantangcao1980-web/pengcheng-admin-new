<template>
  <div class="crm-lead">
    <header class="crm-lead__head">
      <h2>线索管理</h2>
      <div class="actions">
        <input v-model="keyword" placeholder="姓名/公司搜索" @keyup.enter="reload" />
        <button @click="reload">搜索</button>
        <button @click="openCreate">新建线索</button>
        <button @click="openAssign" :disabled="!selected.length">批量分配</button>
      </div>
    </header>

    <table class="crm-lead__table">
      <thead>
        <tr>
          <th><input type="checkbox" v-model="allChecked" @change="toggleAll" /></th>
          <th>编号</th>
          <th>姓名</th>
          <th>电话</th>
          <th>公司</th>
          <th>来源</th>
          <th>意向</th>
          <th>状态</th>
          <th>负责人</th>
          <th>创建时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in list" :key="row.id">
          <td><input type="checkbox" :value="row.id" v-model="selected" /></td>
          <td>{{ row.leadNo }}</td>
          <td>{{ row.name }}</td>
          <td>{{ row.phoneMasked || row.phone }}</td>
          <td>{{ row.company }}</td>
          <td>{{ row.source }}</td>
          <td>{{ intentionLabel(row.intentionLevel) }}</td>
          <td>{{ statusLabel(row.status) }}</td>
          <td>{{ row.ownerId ?? '-' }}</td>
          <td>{{ row.createTime }}</td>
        </tr>
      </tbody>
    </table>

    <div class="crm-lead__empty" v-if="!loading && list.length === 0">暂无线索</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { leadApi, type CrmLead } from '@/api/leadApi'

const list = ref<CrmLead[]>([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const selected = ref<number[]>([])

const allChecked = computed(() => list.value.length > 0 && selected.value.length === list.value.length)

function toggleAll() {
  if (selected.value.length === list.value.length) {
    selected.value = []
  } else {
    selected.value = list.value.map(it => it.id!).filter(Boolean) as number[]
  }
}

async function reload() {
  loading.value = true
  try {
    const res: any = await leadApi.page({ page: 1, size: 50, keyword: keyword.value || undefined })
    list.value = res?.data?.list ?? res?.list ?? []
    total.value = res?.data?.total ?? res?.total ?? 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  // 简化：跳到新建页面（后续可独立路由）
  alert('请在 lead/edit.vue 页面填写创建表单')
}

function openAssign() {
  alert('请在 lead/assign.vue 页面选择目标用户后再调用 leadApi.assign')
}

function intentionLabel(v?: number) {
  return v === 1 ? '高' : v === 3 ? '低' : '中'
}

function statusLabel(v?: number) {
  return ({ 1: '待分配', 2: '已分配', 3: '跟进中', 4: '已转客户', 5: '已废弃' } as any)[v ?? 0] ?? '-'
}

onMounted(reload)
</script>

<style scoped>
.crm-lead { padding: 16px; }
.crm-lead__head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.crm-lead__table { width: 100%; border-collapse: collapse; }
.crm-lead__table th, .crm-lead__table td { padding: 8px 10px; border-bottom: 1px solid #eee; text-align: left; font-size: 13px; }
.crm-lead__empty { text-align: center; color: #999; padding: 32px 0; }
.actions input { margin-right: 8px; padding: 4px 8px; }
.actions button { margin-left: 8px; padding: 4px 12px; cursor: pointer; }
</style>
