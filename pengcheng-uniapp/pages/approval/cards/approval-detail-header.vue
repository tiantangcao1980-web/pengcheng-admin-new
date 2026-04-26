<template>
	<view class="detail-header">
		<view class="row-main">
			<view class="avatar" :style="{ background: typeColor }">
				<text class="avatar-text">{{ typeIcon }}</text>
			</view>
			<view class="main-text">
				<text class="title">{{ item.summary || typeName }}</text>
				<text class="meta">{{ item.applicantName || '未知' }} · {{ formatTime(item.applyTime) }}</text>
			</view>
		</view>
		<view class="row-status">
			<view class="status-tag" :class="statusClass">{{ statusText }}</view>
			<text class="amount" v-if="item.amount">¥ {{ item.amount }}</text>
		</view>
	</view>
</template>

<script>
	/**
	 * 统一详情头组件（D2 任务）。三端可复用。
	 */
	export default {
		name: 'approval-detail-header',
		props: { item: { type: Object, default: () => ({}) } },
		computed: {
			typeName() { return this.item.typeName || '审批事项' },
			typeIcon() {
				const map = { leave: '假', compensate: '调', correction: '补', outing: '外', overtime: '加',
					expense: '报', reimburse: '报', advance: '垫', prepay: '预', commission: '佣', general: '审' }
				return map[this.item.type] || '审'
			},
			typeColor() {
				const map = { leave: '#3f7afe', correction: '#16a085', outing: '#f39c12', overtime: '#e67e22',
					expense: '#e74c3c', reimburse: '#e74c3c', commission: '#2c3e50' }
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
				return s === 2 ? 'success' : (s === 3 || s === 4) ? 'danger' : 'warning'
			},
		},
		methods: {
			formatTime(t) {
				if (!t) return ''
				if (typeof t === 'string') return t.replace('T', ' ').slice(0, 16)
				return String(t)
			},
		},
	}
</script>

<style lang="scss" scoped>
	.detail-header {
		background: linear-gradient(135deg, #4a90e2, #6aa5e8);
		border-radius: 16rpx;
		padding: 32rpx;
		display: flex;
		flex-direction: column;
		gap: 16rpx;
		color: #fff;
	}

	.row-main {
		display: flex;
		gap: 24rpx;
		align-items: center;
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

	.main-text {
		display: flex;
		flex-direction: column;
		gap: 4rpx;
	}

	.title {
		font-size: 32rpx;
		font-weight: 600;
	}

	.meta {
		font-size: 24rpx;
		opacity: 0.85;
	}

	.row-status {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.status-tag {
		padding: 6rpx 16rpx;
		border-radius: 8rpx;
		font-size: 24rpx;

		&.warning { background: rgba(255, 255, 255, 0.2); color: #fff; }
		&.success { background: #52c41a; }
		&.danger { background: #ff4d4f; }
	}

	.amount {
		font-size: 30rpx;
		font-weight: 600;
	}
</style>
