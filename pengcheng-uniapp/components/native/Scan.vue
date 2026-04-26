<template>
  <view class="native-scan">
    <button
      class="native-scan__btn"
      :loading="loading"
      :disabled="loading"
      @tap="handleScan"
    >
      {{ btnLabel }}
    </button>
    <view v-if="error" class="native-scan__error">{{ error }}</view>
    <view v-if="result" class="native-scan__result">
      <text>类型：{{ result.scanType }}</text>
      <text>结果：{{ result.result }}</text>
    </view>
  </view>
</template>

<script>
/**
 * V4.0 闭环⑤ - 扫一扫业务组件
 *
 * Props:
 *   - scanTypes: ['qrCode', 'barCode'] 默认两种都开
 *   - onlyFromCamera: 是否仅相机（默认 true，避免误扫相册）
 *
 * Emits:
 *   - scanned (object)  扫码结果 { result, scanType, charSet }
 *   - error   (object)
 */
import nativeBridge from '@/utils/native-bridge.js'

export default {
  name: 'NativeScan',
  props: {
    scanTypes: {
      type: Array,
      default: () => ['qrCode', 'barCode']
    },
    onlyFromCamera: { type: Boolean, default: true },
    btnLabel: { type: String, default: '扫一扫' }
  },
  emits: ['scanned', 'error'],
  data() {
    return {
      loading: false,
      result: null,
      error: ''
    }
  },
  methods: {
    async handleScan() {
      this.loading = true
      this.error = ''
      try {
        const res = await nativeBridge.scan.scan({
          scanType: this.scanTypes,
          onlyFromCamera: this.onlyFromCamera
        })
        this.result = res
        this.$emit('scanned', res)
      } catch (err) {
        this.error = (err && err.msg) || '扫码失败'
        this.$emit('error', err)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss">
.native-scan {
  padding: 16rpx;

  &__btn {
    background: #faad14;
    color: #fff;
    border-radius: 8rpx;
  }

  &__error {
    margin-top: 12rpx;
    color: #ff4d4f;
    font-size: 24rpx;
  }

  &__result {
    margin-top: 16rpx;
    font-size: 26rpx;

    text {
      display: block;
      line-height: 1.6;
    }
  }
}
</style>
