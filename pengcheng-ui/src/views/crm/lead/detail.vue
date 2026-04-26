<template>
  <div class="lead-detail" v-if="lead">
    <h2>线索详情 #{{ lead.leadNo }}</h2>
    <ul class="kv">
      <li><b>姓名</b><span>{{ lead.name }}</span></li>
      <li><b>电话</b><span>{{ lead.phone }}</span></li>
      <li><b>公司</b><span>{{ lead.company }}</span></li>
      <li><b>来源</b><span>{{ lead.source }} / {{ lead.sourceDetail }}</span></li>
      <li><b>意向</b><span>{{ lead.intentionLevel }}</span></li>
      <li><b>状态</b><span>{{ lead.status }}</span></li>
      <li><b>负责人</b><span>{{ lead.ownerId ?? '-' }}</span></li>
      <li><b>备注</b><span>{{ lead.remark }}</span></li>
    </ul>

    <h3>分配流转记录</h3>
    <table class="hist">
      <thead><tr><th>时间</th><th>由</th><th>分配给</th><th>方式</th><th>备注</th></tr></thead>
      <tbody>
        <tr v-for="it in history" :key="it.id">
          <td>{{ it.createTime }}</td><td>{{ it.fromUserId ?? '-' }}</td>
          <td>{{ it.toUserId }}</td><td>{{ it.ruleType }}</td><td>{{ it.note }}</td>
        </tr>
      </tbody>
    </table>

    <h3>自定义字段</h3>
    <pre>{{ customValues }}</pre>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { leadApi, type CrmLead } from '@/api/leadApi'
import { customFieldApi } from '@/api/customField'

const route = useRoute()
const lead = ref<CrmLead | null>(null)
const history = ref<any[]>([])
const customValues = ref<Record<string, any>>({})

onMounted(async () => {
  const id = Number(route.params.id ?? route.query.id ?? 0)
  if (!id) return
  const res: any = await leadApi.get(id)
  lead.value = res?.data ?? res
  const h: any = await leadApi.assignments(id)
  history.value = h?.data ?? h ?? []
  const v: any = await customFieldApi.loadValues('lead', id)
  customValues.value = v?.data ?? v ?? {}
})
</script>

<style scoped>
.lead-detail { padding: 16px; max-width: 720px; }
.kv { list-style: none; padding: 0; }
.kv li { display: flex; padding: 6px 0; border-bottom: 1px dashed #eee; }
.kv li b { width: 96px; color: #666; font-weight: normal; }
.hist { width: 100%; margin-top: 8px; border-collapse: collapse; font-size: 13px; }
.hist th, .hist td { padding: 6px 8px; border-bottom: 1px solid #eee; text-align: left; }
pre { background: #f7f7f7; padding: 12px; border-radius: 4px; }
</style>
