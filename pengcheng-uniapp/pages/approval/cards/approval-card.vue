<template>
	<view class="approval-card" :class="{ urgent }" @tap="$emit('tap', item)">
		<view class="card-left">
			<view class="avatar" :style="{ background: typeColor }">
				<text class="avatar-text">{{ typeIcon }}</text>
			</view>
		</view>
		<view class="card-body">
			<view class="card-title-row">
				<text class="card-title">{{ item.summary || typeName }}</text>
				<view class="status-tag" :class="statusClass">{{ statusText }}</view>
			</view>
			<text class="card-applicant">{{ item.applicantName || '未知申请人' }}</text>
			<text class="card-meta">{{ formatTime(item.applyTime) }}</text>
			<text v-if="item.amount" class="card-amount">金额：¥ {{ item.amount }}</text>
		</view>
	</view>
</template>

<script>
	/**
	 * 统一审批卡片组件（D2 任务）
	 * - Web 端使用 oaApprovalFlow.ts 渲染同一字段
	 * - APP/MP 端通过此组件复用，避免每个 page 自己实现
	 */
	export default {
		name: 'approval-card',
		props: {
			item: { type: Object, default: () => ({}) },
			urgent: { type: Boolean, default: false },
		},
		computed: {
			typeName() {
				const map = {
					leave: '请假申请',
					compensate: '调休申请',
					correction: '补卡申请',
					outing: '外出申请',
					overtime: '加班申请',
					expense: '费用报销',
					reimburse: '费用报销',
					advance: '垫佣申请',
					prepay: '预付佣',
					commission: '佣金审核',
					general: '通用审批',
				}
				return map[this.item.type] || '审批事项'
			},
			typeIcon() {
				const map = {
					leave: '假',
					compensate: '调',
					correction: '补',
					outing: '外',
					overtime: '加',
					expense: '报',
					reimburse: '报',
					advance: '垫',
					prepay: '预',
					commission: '佣',
					general: '审',
				}
				return map[this.item.type] || '审'
			},
			typeColor() {
				const map = {
					leave: '#3f7afe',
					compensate: '#9b59b6',
					correction: '#16a085',
					outing: '#f39c12',
					overtime: '#e67e22',
					expense: '#e74c3c',
					reimburse: '#e74c3c',
					advance: '#c0392b',
					prepay: '#d35400',
					commission: '#2c3e50',
					general: '#7f8c8d',
				}
				return map[this.item.type] || '#7f8c8d'
			},
			statusText() {
				const s = this.item.status
				if (s === 1) return '待审批'
				if (s === 2) return '已通过'
				if (s === 3) return '已驳回'
				if (s === 4) return '已撤销'
				return '审批中'
			},
			statusClass() {
				const s = this.item.status
				if (s === 2) return 'success'
				if (s === 3 || s === 4) return 'danger'
				return 'warning'
			},
		},
		methods: {
			formatTime(t) {
				if (!t) return ''
				if (typeof t === 'string') return t.replace('T', ' ').slice(0, 16)
				try {
					const d = new Date(t)
					return d.toLocaleString()
				} catch (_) {
					return String(t)
				}
			},
		},
	}
</script>

<style lang="scss" scoped>
	.approval-card {
		display: flex;
		padding: 24rpx;
		background: #fff;
		border-radius: 16rpx;
		margin-bottom: 16rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

		&.urgent {
			border-left: 6rpx solid #e74c3c;
		}
	}

	.card-left {
		margin-right: 24rpx;
	}

	.avatar {
		width: 80rpx;
		height: 80rpx;
		line-height: 80rpx;
		border-radius: 50%;
		text-align: center;
	}

	.avatar-text {
		color: #fff;
		font-size: 32rpx;
	}

	.card-body {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 6rpx;
	}

	.card-title-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.card-title {
		font-size: 30rpx;
		font-weight: 500;
	}

	.card-applicant {
		font-size: 26rpx;
		color: #555;
	}

	.card-meta {
		font-size: 24rpx;
		color: #999;
	}

	.card-amount {
		font-size: 26rpx;
		color: #e74c3c;
	}

	.status-tag {
		font-size: 22rpx;
		padding: 4rpx 12rpx;
		border-radius: 4rpx;

		&.warning {
			background: #fff7e6;
			color: #fa8c16;
		}

		&.success {
			background: #f6ffed;
			color: #52c41a;
		}

		&.danger {
			background: #fff1f0;
			color: #ff4d4f;
		}
	}
</style>
