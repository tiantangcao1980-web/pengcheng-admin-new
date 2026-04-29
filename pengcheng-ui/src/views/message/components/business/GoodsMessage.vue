<template>
  <NCard size="small" class="biz-card biz-card-goods" hoverable>
    <div class="goods-row">
      <div class="thumb">
        <img v-if="data.image" :src="data.image" alt="楼盘" />
        <div v-else class="thumb-placeholder">楼盘</div>
      </div>
      <div class="info">
        <div class="name" :title="data.projectName">{{ data.projectName }}</div>
        <div v-if="data.price" class="price">¥{{ data.price }}</div>
        <div v-if="data.area" class="area">建面 {{ data.area }}</div>
      </div>
    </div>
  </NCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NCard } from 'naive-ui'
import type { BusinessMessagePayload, GoodsPayloadData } from './types'

const props = defineProps<{ payload: BusinessMessagePayload }>()
const data = computed(() => props.payload.data as GoodsPayloadData)
</script>

<style scoped>
.biz-card-goods {
  max-width: 280px;
  border-left: 3px solid #f0a020;
}

.goods-row {
  display: flex;
  gap: 10px;
}

.thumb {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
  background: #f5f7fa;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 12px;
}

.info {
  flex: 1;
  min-width: 0;
}

.name {
  font-weight: 600;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.price {
  font-size: 14px;
  color: #f56c6c;
  margin-top: 4px;
}

.area {
  font-size: 12px;
  color: var(--n-text-color-3, #999);
  margin-top: 2px;
}
</style>
