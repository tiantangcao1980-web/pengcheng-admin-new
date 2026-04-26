<template>
	<view class="page">
		<view class="card">
			<view class="row"><text class="lbl">线索 ID</text><text>{{ leadId }}</text></view>
			<view class="row"><text class="lbl">规则</text>
				<picker :range="rules" range-key="label" @change="e => ruleIdx = e.detail.value">
					<text>{{ rules[ruleIdx].label }}</text>
				</picker>
			</view>
			<view class="row" v-if="rules[ruleIdx].value === 'manual'">
				<text class="lbl">目标用户</text>
				<input v-model.number="targetUserId" type="number" />
			</view>
			<view class="row" v-else>
				<text class="lbl">候选(逗号)</text>
				<input v-model="candidatesRaw" />
			</view>
			<view class="row"><text class="lbl">备注</text><input v-model="note" /></view>
		</view>
		<button class="btn primary" @tap="submit" :disabled="busy">提交</button>
	</view>
</template>

<script>
import { post } from '@/utils/request.js'

export default {
	data() {
		return {
			leadId: null,
			rules: [
				{ label: 'manual 指定', value: 'manual' },
				{ label: 'round_robin 轮询', value: 'round_robin' },
				{ label: 'load_balance 均衡', value: 'load_balance' }
			],
			ruleIdx: 0,
			targetUserId: undefined,
			candidatesRaw: '',
			note: '',
			busy: false
		}
	},
	onLoad(options) {
		this.leadId = Number(options.id || 0)
	},
	methods: {
		async submit() {
			const candidateUserIds = this.candidatesRaw.split(',').map(s => Number(s.trim())).filter(n => !!n)
			this.busy = true
			try {
				await post('/crm/leads/assign', {
					leadIds: [this.leadId],
					ruleType: this.rules[this.ruleIdx].value,
					targetUserId: this.targetUserId,
					candidateUserIds,
					note: this.note
				})
				uni.showToast({ title: '已分配' })
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
