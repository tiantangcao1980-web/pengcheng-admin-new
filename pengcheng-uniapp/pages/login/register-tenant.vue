<template>
	<view class="tenant-register">
		<view class="hero">
			<text class="hero-title">企业一分钟开通</text>
			<text class="hero-desc">三步即可完成：企业信息 → 管理员 → 提交</text>
		</view>

		<view class="card">
			<text class="step-title">{{ stepTitles[step - 1] }}</text>

			<!-- Step 1 企业信息 -->
			<view v-if="step === 1">
				<view class="field">
					<text class="label">企业名称</text>
					<input v-model="form.tenantName" class="input" placeholder="如：彭程信息科技有限公司" />
				</view>
				<view class="field">
					<text class="label">行业</text>
					<input v-model="form.industry" class="input" placeholder="如：realty/internet/retail" />
				</view>
				<view class="field">
					<text class="label">规模</text>
					<input v-model="form.scale" class="input" placeholder="1-50 / 51-200 / 201-1000 / 1000+" />
				</view>
			</view>

			<!-- Step 2 管理员 -->
			<view v-if="step === 2">
				<view class="field">
					<text class="label">管理员用户名</text>
					<input v-model="form.adminUsername" class="input" placeholder="4-20 位字母/数字/下划线" maxlength="20" />
				</view>
				<view class="field">
					<text class="label">密码</text>
					<input v-model="form.adminPassword" class="input" password placeholder="不少于 6 位" />
				</view>
				<view class="field">
					<text class="label">手机号</text>
					<input v-model="form.adminPhone" class="input" type="number" placeholder="11 位手机号" maxlength="11" />
				</view>
				<view class="field">
					<text class="label">邮箱（选填）</text>
					<input v-model="form.adminEmail" class="input" placeholder="可选" />
				</view>
			</view>

			<!-- Step 3 确认 -->
			<view v-if="step === 3">
				<view class="kv"><text class="k">企业</text><text class="v">{{ form.tenantName }}</text></view>
				<view class="kv"><text class="k">行业</text><text class="v">{{ form.industry || '—' }}</text></view>
				<view class="kv"><text class="k">规模</text><text class="v">{{ form.scale || '—' }}</text></view>
				<view class="kv"><text class="k">管理员</text><text class="v">{{ form.adminUsername }}</text></view>
				<view class="kv"><text class="k">手机</text><text class="v">{{ form.adminPhone || '—' }}</text></view>
			</view>

			<view class="actions">
				<button v-if="step > 1" class="btn ghost" @tap="step--">上一步</button>
				<button v-if="step < 3" class="btn primary" @tap="onNext">下一步</button>
				<button v-else class="btn primary" :loading="submitting" @tap="onSubmit">提交注册</button>
			</view>
		</view>
	</view>
</template>

<script setup>
import { reactive, ref } from 'vue'

const stepTitles = ['企业信息', '管理员账号', '确认提交']
const step = ref(1)
const submitting = ref(false)

const form = reactive({
	tenantName: '',
	industry: '',
	scale: '',
	adminUsername: '',
	adminPassword: '',
	adminPhone: '',
	adminEmail: ''
})

function onNext() {
	if (step.value === 1 && !form.tenantName) {
		uni.showToast({ title: '请输入企业名称', icon: 'none' })
		return
	}
	if (step.value === 2) {
		if (!/^[a-zA-Z0-9_]{4,20}$/.test(form.adminUsername)) {
			uni.showToast({ title: '用户名 4-20 位字母数字下划线', icon: 'none' })
			return
		}
		if (!form.adminPassword || form.adminPassword.length < 6) {
			uni.showToast({ title: '密码不少于 6 位', icon: 'none' })
			return
		}
		if (form.adminPhone && !/^1[3-9]\d{9}$/.test(form.adminPhone)) {
			uni.showToast({ title: '手机号格式不合法', icon: 'none' })
			return
		}
	}
	step.value++
}

async function onSubmit() {
	submitting.value = true
	try {
		const baseUrl = uni.getStorageSync('apiBase') || ''
		const res = await uni.request({
			url: baseUrl + '/auth/tenant/register',
			method: 'POST',
			data: form,
			header: { 'Content-Type': 'application/json' }
		})
		if (res.statusCode === 200 && res.data && res.data.code === 200) {
			const result = res.data.data || {}
			uni.showToast({ title: `注册成功 ${result.tenantCode || ''}`, icon: 'success' })
			if (result.login && result.login.token) {
				uni.setStorageSync('token', result.login.token)
				setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 800)
			} else {
				setTimeout(() => uni.navigateTo({ url: '/pages/login/index' }), 800)
			}
		} else {
			uni.showToast({ title: (res.data && res.data.message) || '注册失败', icon: 'none' })
		}
	} catch (e) {
		uni.showToast({ title: '网络错误', icon: 'none' })
	} finally {
		submitting.value = false
	}
}
</script>

<style scoped>
.tenant-register { padding: 32rpx; min-height: 100vh; background: #f6f8fb; }
.hero { padding: 32rpx 16rpx; }
.hero-title { font-size: 44rpx; color: #1f2937; font-weight: 600; display: block; }
.hero-desc { font-size: 26rpx; color: #6b7280; display: block; margin-top: 8rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; box-shadow: 0 4rpx 16rpx rgba(0,0,0,.04); }
.step-title { font-size: 32rpx; color: #1f2937; font-weight: 600; display: block; margin-bottom: 24rpx; }
.field { margin-bottom: 24rpx; }
.label { font-size: 26rpx; color: #6b7280; display: block; margin-bottom: 8rpx; }
.input { background: #f3f4f6; border-radius: 8rpx; padding: 18rpx 20rpx; font-size: 28rpx; }
.kv { display: flex; padding: 16rpx 0; border-bottom: 1px solid #f0f0f0; }
.k { width: 160rpx; color: #6b7280; }
.v { flex: 1; color: #1f2937; }
.actions { display: flex; gap: 16rpx; margin-top: 32rpx; }
.btn { flex: 1; height: 84rpx; line-height: 84rpx; border-radius: 8rpx; font-size: 30rpx; }
.btn.primary { background: #2563eb; color: #fff; }
.btn.ghost { background: #fff; color: #2563eb; border: 1px solid #2563eb; }
</style>
