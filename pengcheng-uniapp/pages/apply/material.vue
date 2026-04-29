<template>
	<view class="page">
		<scroll-view scroll-y class="form-scroll">
			<view class="form-section">
				<view class="form-item">
					<text class="form-label">物料类型 <text class="required">*</text></text>
					<picker :range="materialTypes" range-key="label" @change="onMaterialTypeChange">
						<view class="picker-value">
							<text :class="{ placeholder: !form.materialTypeLabel }">{{ form.materialTypeLabel || '请选择物料类型' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">申请数量 <text class="required">*</text></text>
					<u-input
						v-model="form.quantity"
						type="number"
						placeholder="请输入申请数量"
						:border="false"
						clearable
					></u-input>
				</view>
				<view class="form-item">
					<text class="form-label">用途 <text class="required">*</text></text>
					<u-textarea
						v-model="form.purpose"
						placeholder="请说明物料的具体用途"
						:count="true"
						maxlength="200"
						:border="false"
						auto-height
					></u-textarea>
				</view>
				<view class="form-item">
					<text class="form-label">期望到货日期 <text class="required">*</text></text>
					<picker mode="date" :value="form.expectDate" @change="e => form.expectDate = e.detail.value">
						<view class="picker-value">
							<text :class="{ placeholder: !form.expectDate }">{{ form.expectDate || '请选择期望到货日期' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">备注</text>
					<u-textarea
						v-model="form.remark"
						placeholder="选填，补充其他说明"
						:count="true"
						maxlength="200"
						:border="false"
						auto-height
					></u-textarea>
				</view>
			</view>
		</scroll-view>
		<view class="btn-wrap">
			<u-button
				type="primary"
				color="#07C160"
				shape="circle"
				:loading="submitting"
				:disabled="submitting"
				@click="handleSubmit"
			>
				{{ submitting ? '提交中...' : '提交申请' }}
			</u-button>
		</view>
	</view>
</template>

<script>
	import { submitMaterialApply } from '../../utils/api.js'

	export default {
		data() {
			return {
				form: {
					materialType: '',
					materialTypeLabel: '',
					quantity: '',
					purpose: '',
					expectDate: '',
					remark: ''
				},
				materialTypes: [
					{ label: '办公用品', value: 'office' },
					{ label: '电脑设备', value: 'computer' },
					{ label: '打印耗材', value: 'printer' },
					{ label: '其他', value: 'other' }
				],
				submitting: false
			}
		},
		methods: {
			onMaterialTypeChange(e) {
				const item = this.materialTypes[e.detail.value]
				if (!item) return
				this.form.materialType = item.value
				this.form.materialTypeLabel = item.label
			},
			async handleSubmit() {
				if (!this.form.materialType) return uni.showToast({ title: '请选择物料类型', icon: 'none' })
				const qty = Number(this.form.quantity)
				if (!qty || qty <= 0) return uni.showToast({ title: '请输入有效数量', icon: 'none' })
				if (!this.form.purpose.trim()) return uni.showToast({ title: '请填写用途', icon: 'none' })
				if (!this.form.expectDate) return uni.showToast({ title: '请选择期望到货日期', icon: 'none' })
				this.submitting = true
				try {
					await submitMaterialApply({
						materialType: this.form.materialType,
						materialTypeLabel: this.form.materialTypeLabel,
						quantity: qty,
						purpose: this.form.purpose.trim(),
						expectDate: this.form.expectDate,
						remark: this.form.remark.trim()
					})
					uni.showToast({ title: '提交成功', icon: 'success' })
					setTimeout(() => uni.redirectTo({ url: '/pages/apply/list' }), 1200)
				} catch (err) {
					console.error(err)
				} finally {
					this.submitting = false
				}
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { min-height: 100vh; min-height: 100dvh; background: #F0F0F0; display: flex; flex-direction: column; }
	.form-scroll { flex: 1; }
	.form-section { margin: 20rpx; background: #FFF; border-radius: 16rpx; padding: 8rpx 24rpx; }
	.form-item { padding: 24rpx 0; border-bottom: 1rpx solid #F5F5F5; &:last-child { border-bottom: none; } }
	.form-label { font-size: 26rpx; color: #666; margin-bottom: 12rpx; display: block; }
	.required { color: #F5222D; }
	.picker-value {
		height: 72rpx; display: flex; align-items: center; justify-content: space-between;
		border: 1rpx solid #E8E8E8; border-radius: 8rpx; padding: 0 16rpx;
		font-size: 28rpx; color: #1A1A1A;
	}
	.placeholder { color: #C0C0C0; }
	.btn-wrap { padding: 20rpx 24rpx 40rpx; background: #FFF; }
</style>
