<template>
  <view class="native-geo">
    <button
      class="native-geo__btn"
      :loading="loading"
      :disabled="loading"
      @tap="handleLocate"
    >
      {{ btnText }}
    </button>
    <view v-if="error" class="native-geo__error">{{ error }}</view>
    <view v-if="result" class="native-geo__result">
      <text>纬度：{{ result.latitude }}</text>
      <text>经度：{{ result.longitude }}</text>
      <text v-if="result.address">地址：{{ result.address }}</text>
    </view>
  </view>
</template>

<script>
/**
 * V4.0 闭环⑤ - 定位业务组件
 *
 * 通过 utils/native-bridge.js 封装 uni.getLocation，
 * 暴露 emit('located', payload) 给父组件。
 */
import nativeBridge from '@/utils/native-bridge.js'

export default {
  name: 'NativeGeolocation',
  props: {
    autoLocate: { type: Boolean, default: false },
    highAccuracy: { type: Boolean, default: true },
    btnLabel: { type: String, default: '获取定位' }
  },
  emits: ['located', 'error'],
  data() {
    return {
      loading: false,
      result: null,
      error: ''
    }
  },
  computed: {
    btnText() {
      return this.loading ? '定位中...' : this.btnLabel
    }
  },
  mounted() {
    if (this.autoLocate) {
      this.handleLocate()
    }
  },
  methods: {
    async handleLocate() {
      this.loading = true
      this.error = ''
      try {
        const res = await nativeBridge.location.getOnce({
          enableHighAccuracy: this.highAccuracy
        })
        this.result = res
        this.$emit('located', res)
      } catch (err) {
        this.error = (err && err.msg) || '定位失败，请检查权限'
        this.$emit('error', err)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss">
.native-geo {
  padding: 16rpx;

  &__btn {
    background: #2d8cf0;
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
    color: #333;

    text {
      display: block;
      line-height: 1.6;
    }
  }
}
</style>
