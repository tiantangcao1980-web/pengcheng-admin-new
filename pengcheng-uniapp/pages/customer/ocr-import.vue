<template>
  <view class="ocr-import">
    <!-- 顶部说明 -->
    <view class="ocr-import__tip">
      <text class="ocr-import__tip-text">拍摄或选择名片，自动识别填写客户信息</text>
    </view>

    <!-- 拍照区域 -->
    <view class="ocr-import__camera-box">
      <NativeCamera
        mode="both"
        :count="1"
        btn-label="拍摄名片 / 从相册选择"
        :auto-upload-ocr="false"
        @picked="onImagePicked"
        @error="onCameraError"
      />
      <image
        v-if="previewImagePath"
        :src="previewImagePath"
        mode="aspectFit"
        class="ocr-import__card-preview"
      />
    </view>

    <!-- OCR 状态提示 -->
    <view v-if="ocrLoading" class="ocr-import__loading">
      <text>正在识别名片，请稍候…</text>
    </view>
    <view v-if="ocrError" class="ocr-import__error">
      <text>{{ ocrError }}</text>
      <text class="ocr-import__error-sub">请手动填写以下信息</text>
    </view>

    <!-- 识别结果表单（可编辑） -->
    <view v-if="showForm" class="ocr-import__form">
      <view class="ocr-import__form-title">
        <text>客户信息</text>
        <text v-if="ocrDone && !ocrError" class="ocr-import__form-ocr-badge">OCR 已填充</text>
      </view>

      <view class="ocr-import__field">
        <text class="ocr-import__label">姓名 <text class="required">*</text></text>
        <input
          v-model="form.name"
          class="ocr-import__input"
          placeholder="请输入客户姓名"
          maxlength="20"
        />
      </view>

      <view class="ocr-import__field">
        <text class="ocr-import__label">手机号 <text class="required">*</text></text>
        <input
          v-model="form.phone"
          class="ocr-import__input"
          type="number"
          placeholder="请输入手机号"
          maxlength="11"
        />
      </view>

      <view class="ocr-import__field">
        <text class="ocr-import__label">邮箱</text>
        <input
          v-model="form.email"
          class="ocr-import__input"
          type="email"
          placeholder="请输入邮箱（选填）"
        />
      </view>

      <view class="ocr-import__field">
        <text class="ocr-import__label">公司</text>
        <input
          v-model="form.company"
          class="ocr-import__input"
          placeholder="请输入公司名称（选填）"
          maxlength="50"
        />
      </view>

      <view class="ocr-import__field">
        <text class="ocr-import__label">职位</text>
        <input
          v-model="form.position"
          class="ocr-import__input"
          placeholder="请输入职位（选填）"
          maxlength="30"
        />
      </view>

      <view class="ocr-import__field">
        <text class="ocr-import__label">地址</text>
        <input
          v-model="form.address"
          class="ocr-import__input"
          placeholder="请输入地址（选填）"
          maxlength="100"
        />
      </view>

      <!-- 辅助展示：座机 / 网站（不提交到客户表单主字段，仅供用户参考） -->
      <view v-if="form.telephone || form.website" class="ocr-import__extras">
        <text class="ocr-import__extras-title">名片其他信息（仅供参考，不自动提交）</text>
        <view v-if="form.telephone" class="ocr-import__extra-row">
          <text class="ocr-import__extra-label">座机</text>
          <text class="ocr-import__extra-val">{{ form.telephone }}</text>
        </view>
        <view v-if="form.website" class="ocr-import__extra-row">
          <text class="ocr-import__extra-label">网站</text>
          <text class="ocr-import__extra-val">{{ form.website }}</text>
        </view>
      </view>

      <!-- 提交按钮 -->
      <view class="ocr-import__actions">
        <button
          class="ocr-import__btn-submit"
          :loading="submitting"
          :disabled="submitting"
          @tap="handleSubmit"
        >
          创建客户
        </button>
        <button class="ocr-import__btn-reset" @tap="handleReset">重置表单</button>
      </view>
    </view>

    <!-- 未拍照时的占位提示 -->
    <view v-if="!showForm && !ocrLoading" class="ocr-import__placeholder">
      <text>请先拍摄或选择名片图片</text>
    </view>
  </view>
</template>

<script>
/**
 * 名片 OCR 导入客户页
 *
 * 流程：拍照 → 读取 base64 → 调 OCR 预览接口 → 渲染可编辑表单 → 提交创建客户
 * 失败处理：OCR 失败时保留表单并提示用户手动填写
 */
import NativeCamera from '@/components/native/Camera.vue'
import { ocrBusinessCardPreview, reportCustomer } from '@/utils/api.js'

export default {
  name: 'OcrImport',
  components: { NativeCamera },
  data() {
    return {
      previewImagePath: '',
      ocrLoading: false,
      ocrDone: false,
      ocrError: '',
      showForm: false,
      submitting: false,
      form: {
        name: '',
        phone: '',
        email: '',
        company: '',
        position: '',
        address: '',
        telephone: '',
        website: ''
      }
    }
  },
  methods: {
    // ---------------------------------------------------------------- //
    //  相机回调                                                           //
    // ---------------------------------------------------------------- //

    async onImagePicked(paths) {
      if (!paths || paths.length === 0) return
      const filePath = paths[0]
      this.previewImagePath = filePath
      this.ocrError = ''
      this.ocrDone = false
      this.showForm = true
      this.resetFormFields()
      await this.runOcr(filePath)
    },

    onCameraError(err) {
      uni.showToast({
        title: (err && err.msg) || '拍照失败，请检查相机权限',
        icon: 'none'
      })
    },

    // ---------------------------------------------------------------- //
    //  OCR 识别                                                           //
    // ---------------------------------------------------------------- //

    async runOcr(filePath) {
      this.ocrLoading = true
      try {
        const base64 = await this.readFileAsBase64(filePath)
        const res = await ocrBusinessCardPreview(base64)
        if (res && res.data) {
          this.fillForm(res.data)
          this.ocrDone = true
          this.ocrError = ''
        } else {
          this.ocrError = 'OCR 返回数据为空，请手动填写'
        }
      } catch (err) {
        const msg = (err && err.message) || (err && err.errMsg) || 'OCR 识别失败'
        this.ocrError = msg + '，请手动填写信息'
      } finally {
        this.ocrLoading = false
      }
    },

    readFileAsBase64(filePath) {
      return new Promise((resolve, reject) => {
        uni.getFileSystemManager().readFile({
          filePath,
          encoding: 'base64',
          success: (res) => resolve(res.data),
          fail: (err) => reject(err)
        })
      })
    },

    // ---------------------------------------------------------------- //
    //  表单操作                                                           //
    // ---------------------------------------------------------------- //

    fillForm(preview) {
      this.form.name = preview.name || ''
      this.form.phone = preview.phone || ''
      this.form.email = preview.email || ''
      this.form.company = preview.company || ''
      this.form.position = preview.position || ''
      this.form.address = preview.address || ''
      this.form.telephone = preview.telephone || ''
      this.form.website = preview.website || ''
    },

    resetFormFields() {
      Object.keys(this.form).forEach(key => { this.form[key] = '' })
    },

    handleReset() {
      this.previewImagePath = ''
      this.ocrDone = false
      this.ocrError = ''
      this.showForm = false
      this.resetFormFields()
    },

    async handleSubmit() {
      // 简单前端校验
      if (!this.form.name || !this.form.name.trim()) {
        uni.showToast({ title: '请填写客户姓名', icon: 'none' })
        return
      }
      if (!this.form.phone || !/^1[3-9]\d{9}$/.test(this.form.phone)) {
        uni.showToast({ title: '请填写正确的手机号', icon: 'none' })
        return
      }

      this.submitting = true
      try {
        // 复用现有客户报备接口，name/phone 为必填，其余选填
        await reportCustomer({
          customerName: this.form.name,
          phone: this.form.phone,
          // 以下字段为扩展字段，后端 CustomerCreateDTO 如无对应字段会被忽略
          email: this.form.email || undefined,
          company: this.form.company || undefined,
          position: this.form.position || undefined,
          address: this.form.address || undefined
        })
        uni.showToast({ title: '客户创建成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (err) {
        const msg = (err && err.message) || (err && err.errMsg) || '提交失败，请重试'
        uni.showToast({ title: msg, icon: 'none' })
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style lang="scss">
.ocr-import {
  padding: 24rpx;
  background: #f5f7fa;
  min-height: 100vh;

  &__tip {
    background: #e6f7ff;
    border-radius: 8rpx;
    padding: 16rpx 20rpx;
    margin-bottom: 20rpx;

    &-text {
      font-size: 26rpx;
      color: #1890ff;
    }
  }

  &__camera-box {
    background: #fff;
    border-radius: 12rpx;
    padding: 20rpx;
    margin-bottom: 20rpx;
  }

  &__card-preview {
    width: 100%;
    height: 320rpx;
    border-radius: 8rpx;
    margin-top: 16rpx;
  }

  &__loading {
    text-align: center;
    padding: 24rpx;
    color: #1890ff;
    font-size: 28rpx;
  }

  &__error {
    background: #fff2f0;
    border-radius: 8rpx;
    padding: 16rpx 20rpx;
    margin-bottom: 20rpx;
    color: #ff4d4f;
    font-size: 26rpx;

    &-sub {
      display: block;
      color: #888;
      font-size: 24rpx;
      margin-top: 8rpx;
    }
  }

  &__form {
    background: #fff;
    border-radius: 12rpx;
    padding: 24rpx;

    &-title {
      font-size: 30rpx;
      font-weight: bold;
      color: #333;
      margin-bottom: 20rpx;
      display: flex;
      align-items: center;
      gap: 12rpx;
    }

    &-ocr-badge {
      font-size: 22rpx;
      color: #52c41a;
      background: #f6ffed;
      border: 1rpx solid #b7eb8f;
      padding: 2rpx 10rpx;
      border-radius: 20rpx;
      font-weight: normal;
    }
  }

  &__field {
    margin-bottom: 24rpx;
  }

  &__label {
    display: block;
    font-size: 26rpx;
    color: #555;
    margin-bottom: 10rpx;

    .required {
      color: #ff4d4f;
      margin-left: 4rpx;
    }
  }

  &__input {
    width: 100%;
    height: 72rpx;
    border: 1rpx solid #d9d9d9;
    border-radius: 8rpx;
    padding: 0 20rpx;
    font-size: 28rpx;
    color: #333;
    background: #fafafa;
    box-sizing: border-box;
  }

  &__extras {
    background: #fafafa;
    border-radius: 8rpx;
    padding: 16rpx 20rpx;
    margin-bottom: 24rpx;

    &-title {
      font-size: 24rpx;
      color: #aaa;
      display: block;
      margin-bottom: 12rpx;
    }
  }

  &__extra-row {
    display: flex;
    gap: 16rpx;
    margin-bottom: 8rpx;
  }

  &__extra-label {
    font-size: 26rpx;
    color: #888;
    width: 80rpx;
    flex-shrink: 0;
  }

  &__extra-val {
    font-size: 26rpx;
    color: #555;
    flex: 1;
  }

  &__actions {
    display: flex;
    flex-direction: column;
    gap: 16rpx;
    margin-top: 32rpx;
  }

  &__btn-submit {
    background: #07c160;
    color: #fff;
    border-radius: 8rpx;
    font-size: 30rpx;
    height: 80rpx;
    line-height: 80rpx;
  }

  &__btn-reset {
    background: #fff;
    color: #666;
    border: 1rpx solid #d9d9d9;
    border-radius: 8rpx;
    font-size: 28rpx;
    height: 72rpx;
    line-height: 72rpx;
  }

  &__placeholder {
    text-align: center;
    padding: 60rpx 0;
    color: #bbb;
    font-size: 28rpx;
  }
}
</style>
