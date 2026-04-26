<template>
	<view class="invite-accept">
		<view v-if="loading" class="loading"><text>加载中…</text></view>
		<view v-else-if="!invite" class="empty"><text>未找到该邀请，可能已被撤销或不存在</text></view>
		<view v-else class="card">
			<text class="title">您被邀请加入企业</text>
			<view class="kv"><text class="k">邀请码</text><text class="v">{{ invite.inviteCode }}</text></view>
			<view class="kv"><text class="k">渠道</text><text class="v">{{ invite.channel }}</text></view>
			<view class="kv"><text class="k">手机</text><text class="v">{{ invite.phone || '—' }}</text></view>
			<view class="kv"><text class="k">过期时间</text><text class="v">{{ invite.expiresAt }}</text></view>
			<view class="kv"><text class="k">状态</text><text class="v">{{ statusText }}</text></view>

			<view class="actions">
				<button class="btn primary" :loading="accepting" :disabled="invite.status !== 0" @tap="onAccept">
					接受邀请
				</button>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const invite = ref(null)
const loading = ref(true)
const accepting = ref(false)
const code = ref('')

const STATUS_TEXT = { 0: '待接受', 1: '已接受', 2: '已撤销', 3: '已过期' }
const statusText = computed(() => (invite.value ? STATUS_TEXT[invite.value.status] || '未知' : ''))

function getQuery() {
	// uniapp 在小程序/H5 都通过 onLoad 提供参数，这里兼容 H5 query
	const pages = getCurrentPages ? getCurrentPages() : []
	const current = pages[pages.length - 1]
	return (current && current.options) || {}
}

onMounted(async () => {
	const q = getQuery()
	code.value = q.code || q.c || ''
	if (!code.value) {
		loading.value = false
		return
	}
	try {
		const baseUrl = uni.getStorageSync('apiBase') || ''
		const res = await uni.request({
			url: baseUrl + '/auth/tenant/invite/by-code/' + encodeURIComponent(code.value),
			method: 'GET'
		})
		if (res.statusCode === 200 && res.data && res.data.code === 200) {
			invite.value = res.data.data
		}
	} finally {
		loading.value = false
	}
})

async function onAccept() {
	if (!uni.getStorageSync('token')) {
		uni.showToast({ title: '请先登录后再接受邀请', icon: 'none' })
		setTimeout(() => uni.navigateTo({ url: '/pages/login/index' }), 800)
		return
	}
	accepting.value = true
	try {
		const baseUrl = uni.getStorageSync('apiBase') || ''
		const res = await uni.request({
			url: baseUrl + '/auth/tenant/invite/accept',
			method: 'POST',
			data: { code: code.value },
			header: {
				'Content-Type': 'application/json',
				Authorization: uni.getStorageSync('token')
			}
		})
		if (res.statusCode === 200 && res.data && res.data.code === 200) {
			uni.showToast({ title: '已接受邀请', icon: 'success' })
			invite.value = res.data.data
		} else {
			uni.showToast({ title: (res.data && res.data.message) || '操作失败', icon: 'none' })
		}
	} finally {
		accepting.value = false
	}
}
</script>

<style scoped>
.invite-accept { padding: 32rpx; min-height: 100vh; background: #f6f8fb; }
.loading, .empty { padding: 80rpx 0; text-align: center; color: #6b7280; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; box-shadow: 0 4rpx 16rpx rgba(0,0,0,.04); }
.title { font-size: 36rpx; font-weight: 600; color: #1f2937; display: block; margin-bottom: 24rpx; }
.kv { display: flex; padding: 16rpx 0; border-bottom: 1px solid #f0f0f0; }
.k { width: 160rpx; color: #6b7280; }
.v { flex: 1; color: #1f2937; }
.actions { margin-top: 32rpx; }
.btn { height: 84rpx; line-height: 84rpx; border-radius: 8rpx; font-size: 30rpx; }
.btn.primary { background: #2563eb; color: #fff; }
</style>
