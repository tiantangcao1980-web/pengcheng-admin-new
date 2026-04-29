<template>
  <view class="cfp">
    <!-- loading 态 -->
    <view v-if="loading" class="cfp-loading">
      <text class="cfp-loading-text">加载中...</text>
    </view>

    <!-- 空态 -->
    <view v-else-if="defs.length === 0" class="cfp-empty">
      <text class="cfp-empty-text">暂无自定义字段</text>
    </view>

    <!-- 字段列表 -->
    <template v-else>
      <view class="cfp-fields">
        <view v-for="def in defs" :key="def.id" class="cfp-field-item">
          <text class="cfp-field-label">
            {{ def.label }}<text v-if="def.required === 1" class="cfp-required">*</text>
          </text>

          <!-- text -->
          <u-input
            v-if="def.fieldType === 'text'"
            v-model="fieldValues[def.fieldKey]"
            :placeholder="'请输入' + def.label"
            :disabled="readonly"
          />

          <!-- number -->
          <u-input
            v-else-if="def.fieldType === 'number'"
            v-model="fieldValues[def.fieldKey]"
            type="number"
            :placeholder="'请输入' + def.label"
            :disabled="readonly"
          />

          <!-- date：使用 picker -->
          <picker
            v-else-if="def.fieldType === 'date'"
            mode="date"
            :value="fieldValues[def.fieldKey] || ''"
            :disabled="readonly"
            @change="(e) => (fieldValues[def.fieldKey] = e.detail.value)"
          >
            <view class="cfp-picker-value">
              <text :class="fieldValues[def.fieldKey] ? '' : 'cfp-placeholder'">
                {{ fieldValues[def.fieldKey] || '请选择日期' }}
              </text>
            </view>
          </picker>

          <!-- select (单选) -->
          <view v-else-if="def.fieldType === 'select'">
            <u-radio-group
              v-model="fieldValues[def.fieldKey]"
              :disabled="readonly"
            >
              <u-radio
                v-for="op in parseOptions(def)"
                :key="op.value"
                :label="op.label"
                :name="op.value"
              />
            </u-radio-group>
          </view>

          <!-- multi_select (多选) -->
          <view v-else-if="def.fieldType === 'multi_select'">
            <u-checkbox-group
              v-model="fieldValues[def.fieldKey]"
              :disabled="readonly"
            >
              <u-checkbox
                v-for="op in parseOptions(def)"
                :key="op.value"
                :label="op.label"
                :name="op.value"
              />
            </u-checkbox-group>
          </view>

          <!-- file -->
          <view v-else-if="def.fieldType === 'file'">
            <u-upload
              v-if="!readonly"
              :file-list="toFileList(fieldValues[def.fieldKey])"
              :max-count="1"
              @after-read="(e) => onFileRead(def.fieldKey, e)"
              @delete="() => (fieldValues[def.fieldKey] = '')"
            />
            <text v-else class="cfp-file-url">{{ fieldValues[def.fieldKey] || '-' }}</text>
          </view>

          <text v-else class="cfp-unsupported">不支持类型 {{ def.fieldType }}</text>
        </view>
      </view>

      <!-- 校验错误提示 -->
      <view v-if="errors.length > 0" class="cfp-errors">
        <text v-for="(e, i) in errors" :key="i" class="cfp-error-item">{{ e }}</text>
      </view>

      <!-- 保存按钮 -->
      <view v-if="!readonly" class="cfp-footer">
        <button class="cfp-save-btn" :disabled="saving" @tap="handleSave">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </view>
    </template>
  </view>
</template>

<script>
import { listDefs, getValues, saveValues } from './customFieldApi.js'

export default {
  name: 'CustomFieldsPanel',
  props: {
    entityType: {
      type: String,
      required: true
    },
    entityId: {
      type: [Number, String],
      required: true
    },
    readonly: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false,
      saving: false,
      defs: [],
      fieldValues: {},
      errors: []
    }
  },
  async mounted() {
    await this.load()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const [defRes, valRes] = await Promise.all([
          listDefs(this.entityType),
          getValues(this.entityType, this.entityId)
        ])
        this.defs = defRes?.data ?? defRes ?? []
        const vals = valRes?.data ?? valRes ?? {}
        // 初始化多选字段为数组
        for (const d of this.defs) {
          if (d.fieldType === 'multi_select' && !Array.isArray(vals[d.fieldKey])) {
            vals[d.fieldKey] = vals[d.fieldKey] ? [vals[d.fieldKey]] : []
          }
        }
        this.fieldValues = vals
      } catch (err) {
        console.error('[CustomFieldsPanel] load error', err)
      } finally {
        this.loading = false
      }
    },
    parseOptions(def) {
      if (!def.optionsJson) return []
      try {
        return JSON.parse(def.optionsJson)
      } catch {
        return []
      }
    },
    toFileList(url) {
      if (!url) return []
      return [{ url }]
    },
    onFileRead(fieldKey, event) {
      const file = Array.isArray(event.file) ? event.file[0] : event.file
      if (file && file.url) {
        this.fieldValues[fieldKey] = file.url
      }
    },
    validate() {
      const errs = []
      for (const def of this.defs) {
        if (def.required === 1) {
          const val = this.fieldValues[def.fieldKey]
          const empty =
            val === undefined ||
            val === null ||
            val === '' ||
            (Array.isArray(val) && val.length === 0)
          if (empty) errs.push(`"${def.label}" 为必填项`)
        }
        if (def.fieldType === 'number' && def.validationJson) {
          try {
            const v = JSON.parse(def.validationJson)
            const num = Number(this.fieldValues[def.fieldKey])
            if (v.min !== undefined && num < v.min) errs.push(`"${def.label}" 不能小于 ${v.min}`)
            if (v.max !== undefined && num > v.max) errs.push(`"${def.label}" 不能大于 ${v.max}`)
          } catch { /* noop */ }
        }
      }
      this.errors = errs
      return errs.length === 0
    },
    async handleSave() {
      if (!this.validate()) return
      this.saving = true
      try {
        await saveValues(this.entityType, this.entityId, this.fieldValues)
        uni.showToast({ title: '保存成功', icon: 'success' })
      } catch (err) {
        console.error('[CustomFieldsPanel] save error', err)
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.cfp {
  padding: 12rpx 0;
}
.cfp-loading,
.cfp-empty {
  padding: 32rpx 0;
  text-align: center;
}
.cfp-loading-text,
.cfp-empty-text {
  font-size: 26rpx;
  color: #999;
}
.cfp-fields {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}
.cfp-field-item {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.cfp-field-label {
  font-size: 26rpx;
  color: #555;
}
.cfp-required {
  color: #d4380d;
  margin-left: 4rpx;
}
.cfp-picker-value {
  height: 72rpx;
  display: flex;
  align-items: center;
  border: 1rpx solid #e8e8e8;
  border-radius: 8rpx;
  padding: 0 16rpx;
  font-size: 28rpx;
  color: #1a1a1a;
}
.cfp-placeholder {
  color: #c0c0c0;
}
.cfp-file-url {
  font-size: 24rpx;
  color: #1677ff;
  word-break: break-all;
}
.cfp-unsupported {
  font-size: 22rpx;
  color: #999;
}
.cfp-errors {
  margin-top: 16rpx;
  padding: 16rpx;
  background: #fff2f0;
  border: 1rpx solid #ffccc7;
  border-radius: 8rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.cfp-error-item {
  font-size: 24rpx;
  color: #cf1322;
}
.cfp-footer {
  margin-top: 32rpx;
}
.cfp-save-btn {
  height: 88rpx;
  line-height: 88rpx;
  background: #1677ff;
  color: #fff;
  font-size: 28rpx;
  font-weight: 500;
  border-radius: 12rpx;
  border: none;
}
.cfp-save-btn[disabled] {
  opacity: 0.5;
}
.cfp-save-btn::after {
  border: none;
}
</style>
