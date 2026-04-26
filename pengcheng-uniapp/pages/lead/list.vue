<template>
	<view class="page">
		<view class="filter-bar">
			<input class="filter-input" v-model="keyword" placeholder="搜索姓名/公司" @confirm="reload" />
			<picker :range="statusList" range-key="label" @change="onStatusChange">
				<view class="filter-picker">
					<text>{{ selectedStatusLabel }}</text>
				</view>
			</picker>
		</view>

		<scroll-view scroll-y class="list-scroll" @scrolltolower="loadMore"
			refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="lead-item" v-for="it in list" :key="it.id" @tap="goDetail(it)">
				<view class="item-top">
					<text class="item-name">{{ it.name }}</text>
					<text class="item-phone">{{ it.phoneMasked || it.phone || '' }}</text>
				</view>
				<view class="item-info">
					<text class="info-source">来源：{{ it.source || '-' }}</text>
					<text class="info-status">{{ statusText(it.status) }}</text>
				</view>
				<view class="item-info">
					<text class="info-company">{{ it.company || '-' }}</text>
					<text class="info-time">{{ it.createTime }}</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { get } from '@/utils/request.js'

export default {
	data() {
		return {
			list: [],
			page: 1,
			pageSize: 20,
			keyword: '',
			statusList: [
				{ label: '全部', value: undefined },
				{ label: '待分配', value: 1 },
				{ label: '已分配', value: 2 },
				{ label: '跟进中', value: 3 },
				{ label: '已转客户', value: 4 }
			],
			selectedStatusIdx: 0,
			refreshing: false,
			noMore: false
		}
	},
	computed: {
		selectedStatusLabel() {
			return this.statusList[this.selectedStatusIdx].label
		}
	},
	onShow() {
		this.reload()
	},
	methods: {
		statusText(s) {
			return ({ 1: '待分配', 2: '已分配', 3: '跟进中', 4: '已转客户', 5: '已废弃' })[s] || '-'
		},
		onStatusChange(e) {
			this.selectedStatusIdx = e.detail.value
			this.reload()
		},
		async reload() {
			this.page = 1
			this.noMore = false
			this.list = await this.fetch()
		},
		async loadMore() {
			if (this.noMore) return
			this.page++
			const more = await this.fetch()
			if (more.length === 0) this.noMore = true
			else this.list = this.list.concat(more)
		},
		async onRefresh() {
			this.refreshing = true
			await this.reload()
			this.refreshing = false
		},
		async fetch() {
			const status = this.statusList[this.selectedStatusIdx].value
			const res = await get('/crm/leads', {
				page: this.page,
				size: this.pageSize,
				keyword: this.keyword || undefined,
				status
			})
			const list = res?.data?.list ?? res?.list ?? []
			return list
		},
		goDetail(item) {
			uni.navigateTo({ url: `/pages/lead/detail?id=${item.id}` })
		}
	}
}
</script>

<style scoped>
.page { background: #f5f5f5; min-height: 100vh; }
.filter-bar { display: flex; padding: 12px; background: #fff; gap: 12px; }
.filter-input { flex: 1; padding: 6px 10px; border: 1px solid #eee; border-radius: 4px; }
.filter-picker { color: #555; padding: 6px 8px; }
.list-scroll { height: calc(100vh - 64px); }
.lead-item { background: #fff; padding: 12px 16px; margin: 8px; border-radius: 6px; }
.item-top { display: flex; justify-content: space-between; align-items: center; }
.item-name { font-size: 16px; font-weight: 500; }
.item-phone { font-size: 12px; color: #999; }
.item-info { display: flex; justify-content: space-between; margin-top: 4px; font-size: 12px; color: #666; }
.info-status { color: #1677FF; }
</style>
