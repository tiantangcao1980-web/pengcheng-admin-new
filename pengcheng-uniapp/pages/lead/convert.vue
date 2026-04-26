<template>
	<view class="page">
		<view class="card">
			<view class="row"><text class="lbl">线索 ID</text><text>{{ leadId }}</text></view>
			<view class="row"><text class="lbl">客户 ID</text><input v-model.number="customerId" type="number" placeholder="已存在客户" /></view>
			<view class="row"><text class="lbl">备注</text><input v-model="remark" /></view>
		</view>
		<button class="btn primary" @tap="submit" :disabled="busy">转客户</button>
	</view>
</template>

<script>
import { post } from '@/utils/request.js'

export default {
	data() { return { leadId: null, customerId: undefined, remark: '', busy: false } },
	onLoad(options) { this.leadId = Number(options.id || 0) },
	methods: {
		async submit() {
			this.busy = true
			try {
				await post('/crm/leads/convert', {
					leadId: this.leadId,
					customerId: this.customerId,
					remark: this.remark
				})
				uni.showToast({ title: '已转客户' })
				uni.navigateBack()
			} finally {
				this.busy = false
			}
		}
	}
}
</script>

<style scoped>
.page { padding: 12px; background: #f5f5f5; min-height: 100vh; }
.card { background: #fff; padding: 12px; border-radius: 6px; }
.row { display: flex; padding: 8px 0; border-bottom: 1px dashed #eee; }
.lbl { width: 88px; color: #999; }
input { flex: 1; }
.btn { width: 100%; margin-top: 16px; padding: 10px; border-radius: 4px; }
.btn.primary { background: #1677FF; color: #fff; }
</style>
