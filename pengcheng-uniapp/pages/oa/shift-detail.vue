<template>
	<view class="page">
		<view class="card">
			<text class="title">{{ shift.shiftName || '班次详情' }}</text>
			<view class="row"><text class="label">类型</text><text class="value">{{ typeLabel }}</text></view>
			<view class="row" v-if="shift.shiftType !== 3">
				<text class="label">上班</text><text class="value">{{ shift.startTime || '-' }}</text>
			</view>
			<view class="row" v-if="shift.shiftType !== 3">
				<text class="label">下班</text><text class="value">{{ shift.endTime || '-' }}</text>
			</view>
			<view class="row" v-if="shift.shiftType !== 3">
				<text class="label">迟到容忍</text><text class="value">{{ shift.lateGraceMinutes || 0 }} 分钟</text>
			</view>
			<view class="row" v-if="shift.shiftType !== 3">
				<text class="label">早退容忍</text><text class="value">{{ shift.earlyGraceMinutes || 0 }} 分钟</text>
			</view>
			<view class="row" v-if="shift.shiftType === 3">
				<text class="label">最低工时</text><text class="value">{{ shift.minWorkMinutes || 0 }} 分钟</text>
			</view>
			<view class="row" v-if="shift.remark"><text class="label">备注</text><text class="value">{{ shift.remark }}</text></view>
		</view>

		<view class="action-bar">
			<button type="primary" plain @tap="onApplyCorrection">缺卡？申请补卡</button>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				shift: {},
			}
		},
		computed: {
			typeLabel() {
				const v = this.shift.shiftType
				return v === 1 ? '固定班次' : v === 2 ? '跨夜班次' : v === 3 ? '弹性班次' : '-'
			},
		},
		onLoad(options) {
			if (options && options.data) {
				try {
					this.shift = JSON.parse(decodeURIComponent(options.data))
				} catch (e) {
					this.shift = {}
				}
			}
		},
		methods: {
			onApplyCorrection() {
				uni.navigateTo({ url: '/pages/oa/correction-apply' })
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
		gap: 16rpx;
	}

	.title {
		font-size: 36rpx;
		font-weight: 600;
		margin-bottom: 12rpx;
	}

	.row {
		display: flex;
		justify-content: space-between;
	}

	.label {
		color: #999;
		font-size: 28rpx;
	}

	.value {
		color: #333;
		font-size: 28rpx;
	}

	.action-bar {
		margin-top: 32rpx;
	}
</style>
