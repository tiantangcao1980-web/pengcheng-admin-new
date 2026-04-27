<template>
	<view class="page" v-if="lead">
		<view class="card">
			<view class="card-row"><text class="lbl">编号</text><text class="val">{{ lead.leadNo }}</text></view>
			<view class="card-row"><text class="lbl">姓名</text><text class="val">{{ lead.name }}</text></view>
			<view class="card-row"><text class="lbl">电话</text><text class="val">{{ lead.phone }}</text></view>
			<view class="card-row"><text class="lbl">公司</text><text class="val">{{ lead.company || '-' }}</text></view>
			<view class="card-row"><text class="lbl">来源</text><text class="val">{{ lead.source }}</text></view>
			<view class="card-row"><text class="lbl">状态</text><text class="val">{{ statusText(lead.status) }}</text></view>
			<view class="card-row"><text class="lbl">备注</text><text class="val">{{ lead.remark || '-' }}</text></view>
		</view>

		<view class="actions">
			<button class="btn" @tap="goAssign">分配</button>
			<button class="btn primary" @tap="goConvert">转客户</button>
		</view>

		<view class="card">
			<text class="title">分配流转</text>
			<view class="hist-row" v-for="h in history" :key="h.id">
				<text>{{ h.createTime }}</text>
				<text>{{ h.fromUserId || '-' }} → {{ h.toUserId }}</text>
				<text>{{ h.ruleType }}</text>
			</view>
		</view>

		<!-- 自定义字段 -->
		<view class="card" v-if="id">
			<text class="title">自定义字段</text>
			<custom-fields-panel
				entity-type="lead"
				:entity-id="id"
			/>
		</view>
	</view>
</template>

<script>
import { get } from '@/utils/request.js'
import CustomFieldsPanel from '../../components/custom-field/custom-fields-panel.vue'

export default {
	components: { CustomFieldsPanel },
	data() {
		return { lead: null, history: [], id: 0 }
	},
	onLoad(options) {
		this.id = Number(options.id || 0)
		this.load()
	},
	methods: {
		statusText(s) {
			return ({ 1: '待分配', 2: '已分配', 3: '跟进中', 4: '已转客户', 5: '已废弃' })[s] || '-'
		},
		async load() {
			if (!this.id) return
			const r1 = await get(`/crm/leads/${this.id}`)
			this.lead = r1?.data ?? r1
			const r2 = await get(`/crm/leads/${this.id}/assignments`)
			this.history = r2?.data ?? r2 ?? []
		},
		goAssign() {
			uni.navigateTo({ url: `/pages/lead/assign?id=${this.id}` })
		},
		goConvert() {
			uni.navigateTo({ url: `/pages/lead/convert?id=${this.id}` })
		}
	}
}
</script>

<style scoped>
.page { padding: 12px; background: #f5f5f5; min-height: 100vh; }
.card { background: #fff; padding: 12px; border-radius: 6px; margin-bottom: 12px; }
.card-row { display: flex; padding: 6px 0; border-bottom: 1px dashed #eee; font-size: 14px; }
.lbl { width: 80px; color: #999; }
.val { flex: 1; }
.actions { display: flex; gap: 12px; margin-bottom: 12px; }
.btn { flex: 1; padding: 8px 0; border-radius: 4px; background: #fff; }
.btn.primary { background: #1677FF; color: #fff; }
.title { font-weight: 500; margin-bottom: 8px; display: block; }
.hist-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 12px; color: #555; }
</style>
