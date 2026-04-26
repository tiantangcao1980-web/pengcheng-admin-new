<template>
  <div class="tag-mgr">
    <h2>客户标签管理</h2>
    <div class="form">
      <input v-model="form.tagName" placeholder="标签名" />
      <input v-model="form.color" placeholder="#1677FF" />
      <input v-model="form.category" placeholder="分类（意向/性格/...)" />
      <button @click="save">新建</button>
    </div>

    <div class="tags">
      <span v-for="t in list" :key="t.id" class="chip" :style="{ background: t.color || '#1677FF' }">
        {{ t.tagName }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { crmTagApi, type CustomerTag } from '@/api/crmTag'

const list = ref<CustomerTag[]>([])
const form = reactive<CustomerTag>({ tagName: '', color: '#1677FF' })

async function reload() {
  const res: any = await crmTagApi.list()
  list.value = res?.data ?? res ?? []
}
async function save() {
  if (!form.tagName) return
  await crmTagApi.create({ ...form })
  form.tagName = ''
  await reload()
}
onMounted(reload)
</script>

<style scoped>
.tag-mgr { padding: 16px; }
.form { display: flex; gap: 8px; margin-bottom: 16px; }
.form input { padding: 4px 8px; }
.form button { padding: 4px 12px; }
.tags .chip { display: inline-block; padding: 4px 12px; color: #fff; border-radius: 12px; margin: 4px 8px 4px 0; font-size: 12px; }
</style>
