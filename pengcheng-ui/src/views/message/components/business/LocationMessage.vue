<template>
  <NCard size="small" class="biz-card biz-card-location" hoverable>
    <div class="map-placeholder">
      <NIcon size="28" color="#fb7299">
        <!-- 占位 SVG（避免依赖 @vicons 在此组件路径中） -->
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 010-5 2.5 2.5 0 010 5z" />
        </svg>
      </NIcon>
    </div>
    <div class="address" :title="data.address">
      {{ data.address || `${data.lat?.toFixed?.(4)}, ${data.lng?.toFixed?.(4)}` }}
    </div>
    <NButton size="tiny" tertiary block @click="onNavigate">
      查看位置
    </NButton>
  </NCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NButton, NCard, NIcon, useMessage } from 'naive-ui'
import type { BusinessMessagePayload, LocationPayloadData } from './types'

const props = defineProps<{ payload: BusinessMessagePayload }>()
const data = computed(() => props.payload.data as LocationPayloadData)
const message = useMessage()

function onNavigate() {
  // V1.0 占位：未来对接高德/腾讯地图打开导航
  message.info(`经纬度：${data.value.lng}, ${data.value.lat}`)
}
</script>

<style scoped>
.biz-card-location {
  max-width: 240px;
  border-left: 3px solid #fb7299;
}

.map-placeholder {
  height: 88px;
  background: linear-gradient(135deg, #fff5f8 0%, #ffe7ee 100%);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.address {
  font-size: 13px;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
