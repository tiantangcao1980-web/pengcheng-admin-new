<template>
	<view class="page">
		<scroll-view scroll-y class="form-scroll">
			<view class="form-section">
				<view class="form-item">
					<text class="form-label">用车类型 <text class="required">*</text></text>
					<picker :range="carTypes" range-key="label" @change="onCarTypeChange">
						<view class="picker-value">
							<text :class="{ placeholder: !form.carTypeLabel }">{{ form.carTypeLabel || '请选择用车类型' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">出发地 <text class="required">*</text></text>
					<u-input
						v-model="form.fromLocation"
						placeholder="请输入出发地"
						:border="false"
						clearable
					></u-input>
				</view>
				<view class="form-item">
					<text class="form-label">目的地 <text class="required">*</text></text>
					<u-input
						v-model="form.toLocation"
						placeholder="请输入目的地"
						:border="false"
						clearable
					></u-input>
				</view>
				<view class="form-item">
					<text class="form-label">用车日期 <text class="required">*</text></text>
					<picker mode="date" :value="form.useDate" @change="e => form.useDate = e.detail.value">
						<view class="picker-value">
							<text :class="{ placeholder: !form.useDate }">{{ form.useDate || '请选择用车日期' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">用车时间 <text class="required">*</text></text>
					<picker mode="time" :value="form.useTime" @change="e => form.useTime = e.detail.value">
						<view class="picker-value">
							<text :class="{ placeholder: !form.useTime }">{{ form.useTime || '请选择用车时间' }}</text>
							<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
						</view>
					</picker>
				</view>
				<view class="form-item">
					<text class="form-label">同行人数 <text class="required">*</text></text>
					<u-input
						v-model="form.peopleCount"
						type="number"
						placeholder="请输入同行人数"
						:border="false"
						clearable
					></u-input>
				</view>
				<view class="form-item">
					<text class="form-label">用车原因 <text class="required">*</text></text>
					<u-textarea
						v-model="form.reason"
						placeholder="请说明本次用车原因"
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
	import { submitCarApply } from '../../utils/api.js'

	export default {
		data() {
			return {
				form: {
					carType: '',
					carTypeLabel: '',
					fromLocation: '',
					toLocation: '',
					useDate: '',
					useTime: '',
					peopleCount: '1',
					reason: ''
				},
				carTypes: [
					{ label: '商务接待', value: 'business' },
					{ label: '客户带看', value: 'customer-visit' },
					{ label: '外勤拜访', value: 'field-work' },
					{ label: '其他', value: 'other' }
				],
				submitting: false
			}
		},
		methods: {
			onCarTypeChange(e) {
				const item = this.carTypes[e.detail.value]
				if (!item) return
				this.form.carType = item.value
				this.form.carTypeLabel = item.label
			},
			async handleSubmit() {
				if (!this.form.carType) return uni.showToast({ title: '请选择用车类型', icon: 'none' })
				if (!this.form.fromLocation.trim()) return uni.showToast({ title: '请输入出发地', icon: 'none' })
				if (!this.form.toLocation.trim()) return uni.showToast({ title: '请输入目的地', icon: 'none' })
				if (!this.form.useDate) return uni.showToast({ title: '请选择用车日期', icon: 'none' })
				if (!this.form.useTime) return uni.showToast({ title: '请选择用车时间', icon: 'none' })
				const people = Number(this.form.peopleCount)
				if (!people || people <= 0) return uni.showToast({ title: '请输入有效同行人数', icon: 'none' })
				if (!this.form.reason.trim()) return uni.showToast({ title: '请填写用车原因', icon: 'none' })
				this.submitting = true
				try {
					await submitCarApply({
						carType: this.form.carType,
						carTypeLabel: this.form.carTypeLabel,
						fromLocation: this.form.fromLocation.trim(),
						toLocation: this.form.toLocation.trim(),
						useDate: this.form.useDate,
						useTime: this.form.useTime,
						peopleCount: people,
						reason: this.form.reason.trim()
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
