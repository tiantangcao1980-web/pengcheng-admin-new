<template>
  <NSpace vertical :size="16">
    <NPageHeader title="报表下载中心" subtitle="导出销售业绩 / 客户分析 / 佣金明细 / 客户跟进报表" />

    <NCard size="small" title="筛选条件">
      <NSpace>
        <NDatePicker
          v-model:value="dateRange"
          type="daterange"
          clearable
          format="yyyy-MM-dd"
          style="width: 280px"
        />
      </NSpace>
    </NCard>

    <NGrid :cols="2" :x-gap="12" :y-gap="12">
      <NGi v-for="t in reportTypes" :key="t.code">
        <NCard :title="t.label" size="small" hoverable>
          <NSpace vertical>
            <span class="desc">{{ t.desc }}</span>
            <NButton type="primary" size="small" @click="onDownload(t.code)">
              下载报表
            </NButton>
          </NSpace>
        </NCard>
      </NGi>
    </NGrid>

    <NAlert type="info" title="说明" :show-icon="false">
      报表文件接口由后端 <code>/admin/report/file/download</code> 提供，下载即触发 Excel 导出。
    </NAlert>
  </NSpace>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  NAlert,
  NButton,
  NCard,
  NDatePicker,
  NGi,
  NGrid,
  NPageHeader,
  NSpace,
  useMessage
} from 'naive-ui'
import {
  realtyApi,
  REPORT_FILE_TYPE_LABEL,
  type ReportFileType
} from '@/api/realty'

const message = useMessage()
const dateRange = ref<[number, number] | null>(null)

const reportTypes: Array<{ code: ReportFileType; label: string; desc: string }> = [
  { code: 'sales-performance', label: REPORT_FILE_TYPE_LABEL['sales-performance'], desc: '按周期汇总销售签约 / 拜访 / 转化数据' },
  { code: 'customer-analysis', label: REPORT_FILE_TYPE_LABEL['customer-analysis'], desc: '客户来源 / 状态 / 公海池分布分析' },
  { code: 'commission-list', label: REPORT_FILE_TYPE_LABEL['commission-list'], desc: '佣金应收应付 / 审批状态明细' },
  { code: 'customer-followup-report', label: REPORT_FILE_TYPE_LABEL['customer-followup-report'], desc: '客户跟进记录 / 下次跟进提醒清单' }
]

const dateRangeStr = computed<[string?, string?]>(() => {
  if (!dateRange.value) return [undefined, undefined]
  const [s, e] = dateRange.value
  const fmt = (ts: number) => new Date(ts).toISOString().slice(0, 10)
  return [fmt(s), fmt(e)]
})

async function onDownload(type: ReportFileType) {
  try {
    const [startDate, endDate] = dateRangeStr.value
    const blob = await realtyApi.reportFileDownload(type, startDate, endDate)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${REPORT_FILE_TYPE_LABEL[type]}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
    message.success('下载已开始')
  } catch (e: any) {
    message.error(e?.message || '下载失败')
  }
}
</script>

<style scoped>
.desc {
  font-size: 12px;
  color: var(--n-text-color-3, #999);
}
</style>
