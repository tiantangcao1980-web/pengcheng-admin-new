<template>
	<view class="page">
		<view class="card">
			<text class="title">审批流程</text>
			<view class="step-list">
				<view class="step" v-for="(node, idx) in nodes" :key="node.id || idx" :class="getStepClass(node, idx)">
					<view class="step-dot">{{ idx + 1 }}</view>
					<view class="step-body">
						<text class="step-name">{{ node.nodeName }}</text>
						<text class="step-meta">{{ approverLabel(node) }}</text>
					</view>
					<view class="step-status" v-if="getStepStatus(node, idx)">
						{{ getStepStatus(node, idx) }}
					</view>
				</view>
			</view>
			<view v-if="!nodes.length" class="empty">该流程未配置节点</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				instanceId: null,
				instance: {},
				nodes: [],
				records: [],
			}
		},
		onLoad(options) {
			this.instanceId = options && options.id
			if (options && options.payload) {
				try {
					const data = JSON.parse(decodeURIComponent(options.payload))
					this.instance = data.instance || {}
					this.records = data.records || []
					this.nodes = data.nodes || []
				} catch (e) {
					/* ignore */
				}
			}
		},
		methods: {
			approverLabel(node) {
				if (!node) return ''
				if (node.nodeType === 2) return '部门主管'
				if (node.nodeType === 3) return '角色: ' + (node.roleKey || '-')
				return '审批人: ' + (node.approverIds || '-')
			},
			getStepClass(node, idx) {
				const cur = this.instance.currentNodeOrder
				if (cur == null) return ''
				const order = node.nodeOrder != null ? node.nodeOrder : idx + 1
				if (order < cur) return 'done'
				if (order === cur) return 'current'
				return 'pending'
			},
			getStepStatus(node, idx) {
				const order = node.nodeOrder != null ? node.nodeOrder : idx + 1
				const rec = this.records.find(r => r.nodeOrder === order)
				if (!rec) return ''
				if (rec.result === 1) return '已通过'
				if (rec.result === 2) return '已驳回'
				if (rec.result === 3) return '超时通过'
				if (rec.result === 4) return '超时驳回'
				if (rec.result === 5) return '超时跳过'
				return ''
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
	}

	.title {
		font-size: 36rpx;
		font-weight: 600;
		margin-bottom: 24rpx;
	}

	.step {
		display: flex;
		align-items: center;
		padding: 20rpx 0;
		border-bottom: 1px solid #f0f0f0;
	}

	.step-dot {
		width: 56rpx;
		height: 56rpx;
		line-height: 56rpx;
		text-align: center;
		border-radius: 50%;
		background: #e5e7eb;
		color: #555;
		margin-right: 24rpx;
	}

	.step.done .step-dot {
		background: #52c41a;
		color: #fff;
	}

	.step.current .step-dot {
		background: #4a90e2;
		color: #fff;
	}

	.step-body {
		flex: 1;
		display: flex;
		flex-direction: column;
	}

	.step-name {
		font-size: 30rpx;
	}

	.step-meta {
		font-size: 24rpx;
		color: #999;
	}

	.step-status {
		font-size: 24rpx;
		color: #4a90e2;
	}

	.empty {
		text-align: center;
		padding: 48rpx;
		color: #999;
	}
</style>
