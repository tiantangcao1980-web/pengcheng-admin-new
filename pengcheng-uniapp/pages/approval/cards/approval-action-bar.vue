<template>
	<view class="action-bar" v-if="!hidden">
		<button class="btn btn-reject" @tap="onReject" :disabled="submitting">驳回</button>
		<button class="btn btn-approve" @tap="onApprove" :disabled="submitting">通过</button>
	</view>
</template>

<script>
	/**
	 * 统一审批处理操作条（D2 任务）。
	 * 通过 emit 通过/驳回事件，由父页面调用对应 API。
	 */
	export default {
		name: 'approval-action-bar',
		props: {
			submitting: { type: Boolean, default: false },
			hidden: { type: Boolean, default: false },
		},
		methods: {
			onApprove() { this.$emit('approve') },
			onReject() { this.$emit('reject') },
		},
	}
</script>

<style lang="scss" scoped>
	.action-bar {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		display: flex;
		gap: 0;
		background: #fff;
		padding: 16rpx 24rpx env(safe-area-inset-bottom);
		box-shadow: 0 -2rpx 8rpx rgba(0, 0, 0, 0.06);
	}

	.btn {
		flex: 1;
		font-size: 30rpx;
		border-radius: 8rpx;
		margin: 0 8rpx;
	}

	.btn-approve {
		background: #4a90e2;
		color: #fff;
	}

	.btn-reject {
		background: #fff;
		color: #e74c3c;
		border: 1px solid #e74c3c;
	}
</style>
