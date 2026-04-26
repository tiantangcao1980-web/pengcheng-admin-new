<template>
  <view class="native-camera">
    <button
      class="native-camera__btn"
      :loading="loading"
      :disabled="loading"
      @tap="handleTake"
    >
      {{ btnLabel }}
    </button>
    <view v-if="error" class="native-camera__error">{{ error }}</view>
    <view v-if="paths.length" class="native-camera__preview">
      <image
        v-for="(p, idx) in paths"
        :key="idx"
        :src="p"
        mode="aspectFill"
        class="native-camera__img"
      />
    </view>
  </view>
</template>

<script>
/**
 * V4.0 闭环⑤ - 相机/选图业务组件
 *
 * Props:
 *   - mode: 'camera' | 'album' | 'both'  默认 both
 *   - count: 最大张数（默认 1）
 *   - autoUploadOcr: 是否将拍到的第一张自动调后端 OCR（用于名片识别）
 *
 * Emits:
 *   - picked    (string[])  拍到 / 选到的临时文件路径数组
 *   - ocr       (object)    OCR 名片字段（仅 autoUploadOcr=true）
 *   - error     (object)
 */
import nativeBridge from '@/utils/native-bridge.js'

export default {
  name: 'NativeCamera',
  props: {
    mode: { type: String, default: 'both' },
    count: { type: Number, default: 1 },
    btnLabel: { type: String, default: '拍照 / 选图' },
    autoUploadOcr: { type: Boolean, default: false },
    ocrUploadUrl: { type: String, default: '/api/system/ocr/business-card' }
  },
  emits: ['picked', 'ocr', 'error'],
  data() {
    return {
      loading: false,
      paths: [],
      error: ''
    }
  },
  computed: {
    sourceType() {
      if (this.mode === 'camera') return ['camera']
      if (this.mode === 'album') return ['album']
      return ['album', 'camera']
    }
  },
  methods: {
    async handleTake() {
      this.loading = true
      this.error = ''
      try {
        const paths = await nativeBridge.camera.pickImage({
          count: this.count,
          sourceType: this.sourceType
        })
        this.paths = paths
        this.$emit('picked', paths)
        if (this.autoUploadOcr && paths.length > 0) {
          await this.runOcr(paths[0])
        }
      } catch (err) {
        this.error = (err && err.msg) || '拍照失败，请检查相机权限'
        this.$emit('error', err)
      } finally {
        this.loading = false
      }
    },
    runOcr(filePath) {
      return new Promise((resolve, reject) => {
        uni.uploadFile({
          url: this.ocrUploadUrl,
          filePath,
          name: 'file',
          success: (res) => {
            try {
              const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
              this.$emit('ocr', data)
              resolve(data)
            } catch (ex) {
              this.$emit('error', { code: 'OCR_PARSE', msg: String(ex) })
              reject(ex)
            }
          },
          fail: (err) => {
            this.$emit('error', { code: 'OCR_UPLOAD_FAIL', msg: err && err.errMsg, raw: err })
            reject(err)
          }
        })
      })
    }
  }
}
</script>

<style lang="scss">
.native-camera {
  padding: 16rpx;

  &__btn {
    background: #52c41a;
    color: #fff;
    border-radius: 8rpx;
  }

  &__error {
    margin-top: 12rpx;
    color: #ff4d4f;
    font-size: 24rpx;
  }

  &__preview {
    margin-top: 16rpx;
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
  }

  &__img {
    width: 200rpx;
    height: 200rpx;
    border-radius: 8rpx;
  }
}
</style>
