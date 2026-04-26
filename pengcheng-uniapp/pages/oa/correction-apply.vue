<template>
	<view class="page">
		<view class="card">
			<text class="title">补卡申请</text>
			<view class="form-item">
				<text class="form-label">补卡日期</text>
				<picker mode="date" :value="form.correctionDate" @change="onDateChange">
					<view class="picker-value">{{ form.correctionDate || '请选择日期' }}</view>
				</picker>
			</view>
			<view class="form-item">
				<text class="form-label">补卡类型</text>
				<view class="radio-row">
					<view class="radio-item" :class="{ active: form.correctionType === 1 }" @tap="form.correctionType = 1">上班</view>
					<view class="radio-item" :class="{ active: form.correctionType === 2 }" @tap="form.correctionType = 2">下班</view>
				</view>
			</view>
			<view class="form-item">
				<text class="form-label">应打卡时间</text>
				<input v-model="form.expectedTime" placeholder="YYYY-MM-DD HH:mm:ss" class="form-input" />
			</view>
			<view class="form-item">
				<text class="form-label">原因</text>
				<textarea v-model="form.reason" placeholder="请说明补卡原因" class="form-textarea" />
			</view>
		</view>
		<view class="action-bar">
			<button type="primary" :loading="submitting" @tap="submit">提交补卡申请</button>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				submitting: false,
				form: {
					correctionDate: '',
					correctionType: 1,
					expectedTime: '',
					reason: '',
				},
			}
		},
		methods: {
			onDateChange(e) {
				this.form.correctionDate = e.detail.value
			},
			async submit() {
				if (!this.form.correctionDate || !this.form.expectedTime) {
					uni.showToast({ title: '请填写日期与时间', icon: 'none' })
					return
				}
				this.submitting = true
				try {
					// 通过现有 utils/api.js 入口提交
					const api = await import('../../utils/api.js')
					const fn = api.submitCorrection || api.default?.submitCorrection
					if (typeof fn === 'function') {
						await fn(this.form)
					} else {
						// 退化：直接 uni.request
						const baseUrl = (uni.getStorageSync('baseUrl') || '') + '/admin/oa/corrections'
						await new Promise((resolve, reject) => {
							uni.request({
								url: baseUrl,
								method: 'POST',
								data: this.form,
								success: resolve,
								fail: reject,
							})
						})
					}
					uni.showToast({ title: '已提交' })
					setTimeout(() => uni.navigateBack(), 800)
				} catch (e) {
					uni.showToast({ title: '提交失败', icon: 'none' })
				} finally {
					this.submitting = false
				}
			},
		},
	}
</script>

<style lang="scss" scoped>
	.page {
		padding: 24rpx;
	}

	.card {
		background: #fff;
		border-radius: 16rpx;
		padding: 32rpx;
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.title {
		font-size: 36rpx;
		font-weight: 600;
	}

	.form-label {
		font-size: 28rpx;
		color: #555;
		display: block;
		margin-bottom: 12rpx;
	}

	.form-input,
	.form-textarea,
	.picker-value {
		background: #f5f7fa;
		border-radius: 8rpx;
		padding: 16rpx;
		font-size: 28rpx;
	}

	.form-textarea {
		min-height: 120rpx;
	}

	.radio-row {
		display: flex;
		gap: 16rpx;
	}

	.radio-item {
		flex: 1;
		text-align: center;
		padding: 18rpx;
		border-radius: 8rpx;
		background: #f5f7fa;
	}

	.radio-item.active {
		background: #4a90e2;
		color: #fff;
	}

	.action-bar {
		margin-top: 32rpx;
	}
</style>
