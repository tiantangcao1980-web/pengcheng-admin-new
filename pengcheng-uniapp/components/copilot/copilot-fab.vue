<template>
  <view v-if="visible" class="copilot-fab" @tap="onTap" :style="positionStyle">
    <text class="copilot-fab-text">AI</text>
  </view>
</template>

<script>
/**
 * V4.0 MVP 闭环④ — uniapp Copilot 悬浮按钮。
 * - 跳转到 pages/ai/copilot 新页面（不修改现有 chat.vue）
 * - 可通过 props.hidePages 隐藏在登录页等
 */
export default {
  name: 'CopilotFab',
  props: {
    bottom: { type: Number, default: 120 },
    right: { type: Number, default: 24 },
    hidePages: {
      type: Array,
      default: () => ['pages/login/index']
    }
  },
  data() {
    return { visible: true }
  },
  computed: {
    positionStyle() {
      return `bottom:${this.bottom}rpx;right:${this.right}rpx;`
    }
  },
  mounted() {
    try {
      const pages = getCurrentPages()
      const route = pages?.[pages.length - 1]?.route
      if (route && this.hidePages.includes(route)) this.visible = false
    } catch (_) { /* ignore */ }
  },
  methods: {
    onTap() {
      uni.navigateTo({ url: '/pages/ai/copilot' })
    }
  }
}
</script>

<style scoped>
.copilot-fab {
  position: fixed;
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #111827 0%, #374151 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 32rpx rgba(17, 24, 39, 0.32);
  z-index: 9998;
}
.copilot-fab-text {
  color: #fff;
  font-weight: 700;
  font-size: 28rpx;
  letter-spacing: 1rpx;
}
</style>
