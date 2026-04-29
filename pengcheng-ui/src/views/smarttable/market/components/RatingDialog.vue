<template>
  <n-modal v-model:show="visible" preset="card" title="为模板评分" style="width: 440px">
    <n-form :model="form" label-placement="left" label-width="60">
      <n-form-item label="评分" required>
        <n-rate v-model:value="form.rating" :count="5" size="large" />
        <span class="rating-hint" v-if="form.rating">{{ ratingLabel }}</span>
      </n-form-item>
      <n-form-item label="短评">
        <n-input
          v-model:value="form.review"
          type="textarea"
          placeholder="说说你的使用感受（可选，最多 500 字）"
          :rows="4"
          :maxlength="500"
          show-count
        />
      </n-form-item>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button @click="visible = false">取消</n-button>
        <n-button
          type="primary"
          :loading="submitting"
          :disabled="!form.rating"
          @click="handleSubmit"
        >
          提交评分
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { NModal, NForm, NFormItem, NRate, NInput, NButton, NSpace, useMessage } from 'naive-ui'
import { smartTableMarketApi } from '@/api/smartTableMarket'

const props = defineProps<{
  templateId: number | null
}>()

const emit = defineEmits<{
  (e: 'rated'): void
}>()

const message = useMessage()
const visible = defineModel<boolean>('show', { default: false })

const form = ref({ rating: 0, review: '' })
const submitting = ref(false)

const ratingLabel = computed(() => {
  const labels: Record<number, string> = { 1: '很差', 2: '较差', 3: '一般', 4: '好用', 5: '非常棒' }
  return labels[form.value.rating] || ''
})

// 打开时重置表单
watch(visible, (val) => {
  if (val) {
    form.value = { rating: 0, review: '' }
  }
})

async function handleSubmit() {
  if (!props.templateId || !form.value.rating) return
  submitting.value = true
  try {
    await smartTableMarketApi.rateTemplate(props.templateId, {
      rating: form.value.rating,
      review: form.value.review || undefined
    })
    message.success('评分提交成功，感谢你的反馈！')
    visible.value = false
    emit('rated')
  } catch {
    // request 拦截器已弹错
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.rating-hint {
  margin-left: 12px;
  font-size: 13px;
  color: #f0a020;
  font-weight: 500;
}
</style>
