<template>
  <div class="ie">
    <h2>线索 Excel 导入 / 导出</h2>
    <div class="actions">
      <a :href="templateUrl" target="_blank">下载导入模板</a>
      <input type="file" accept=".xlsx,.xls" @change="onFile" />
      <button @click="upload" :disabled="!file || busy">开始导入</button>
    </div>

    <section v-if="result" class="result">
      <p>共 {{ result.total }} 行：成功 {{ result.success }}，失败 {{ result.failed }}</p>
      <table v-if="result.failedRows.length">
        <thead><tr><th>行号</th><th>错误</th><th>原行</th></tr></thead>
        <tbody>
          <tr v-for="r in result.failedRows" :key="r.rowNum">
            <td>{{ r.rowNum }}</td>
            <td>{{ r.message }}</td>
            <td><code>{{ JSON.stringify(r.row) }}</code></td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { crmImportApi, type ImportResult } from '@/api/crmImport'

const file = ref<File | null>(null)
const busy = ref(false)
const result = ref<ImportResult | null>(null)
const templateUrl = crmImportApi.templateUrl()

function onFile(e: Event) {
  const t = e.target as HTMLInputElement
  file.value = t.files && t.files[0] ? t.files[0] : null
}

async function upload() {
  if (!file.value) return
  busy.value = true
  try {
    const res: any = await crmImportApi.importLeads(file.value)
    result.value = res?.data ?? res
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.ie { padding: 16px; }
.actions { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
.result table { width: 100%; border-collapse: collapse; }
.result th, .result td { padding: 6px 10px; border-bottom: 1px solid #eee; font-size: 13px; text-align: left; }
</style>
