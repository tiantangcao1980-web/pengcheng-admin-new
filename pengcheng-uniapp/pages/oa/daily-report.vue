<template>
	<view class="page">
		<view class="card">
			<text class="title">今日日报</text>

			<view class="form-item">
				<text class="form-label">日报内容</text>
				<textarea v-model="form.content" placeholder="今天做了什么 / 明天的计划 / 遇到的问题" class="form-textarea" />
				<view class="quick-actions">
					<button class="quick-btn" :loading="recording" @tap="onVoiceInput">
						<text>{{ recording ? '识别中...' : '🎤 语音输入' }}</text>
					</button>
					<button class="quick-btn" :loading="aiLoading" @tap="onAiDraft">
						<text>{{ aiLoading ? 'AI 写作中...' : '✨ AI 草稿' }}</text>
					</button>
				</view>
			</view>

			<view class="form-item">
				<text class="form-label">明日计划</text>
				<textarea v-model="form.plan" placeholder="计划做什么" class="form-textarea" />
			</view>

			<view class="form-item">
				<text class="form-label">问题/需要协助</text>
				<textarea v-model="form.issues" placeholder="可空" class="form-textarea" />
			</view>
		</view>

		<view class="action-bar">
			<button type="primary" :loading="submitting" @tap="submit">提交日报</button>
		</view>
	</view>
</template>

<script>
	/**
	 * 日报移动端体验（D2 任务）：
	 * - 语音输入按钮：调用 uni.getRecorderManager 录音 → 上传现有 ASR 后端
	 * - AI 草稿按钮：调用现有 AI 接口生成内容
	 *   不修改 AI 后端，仅做 UI 与现有 endpoint 对接（fallback 失败优雅降级为用户手填）
	 */
	export default {
		data() {
			return {
				submitting: false,
				recording: false,
				aiLoading: false,
				recorder: null,
				form: {
					content: '',
					plan: '',
					issues: '',
				},
			}
		},
		onLoad() {
			try {
				this.recorder = uni.getRecorderManager()
				this.recorder.onStop((res) => this.handleRecord(res))
				this.recorder.onError(() => {
					this.recording = false
					uni.showToast({ title: '录音失败', icon: 'none' })
				})
			} catch (_) {
				/* H5 不支持 RecorderManager */
			}
		},
		methods: {
			onVoiceInput() {
				if (!this.recorder) {
					uni.showToast({ title: '当前平台不支持录音', icon: 'none' })
					return
				}
				if (this.recording) {
					this.recording = false
					this.recorder.stop()
				} else {
					this.recording = true
					this.recorder.start({ duration: 60000, format: 'mp3', sampleRate: 16000 })
					uni.showToast({ title: '录音中，再次点击结束', icon: 'none' })
				}
			},
			async handleRecord(res) {
				this.recording = false
				if (!res || !res.tempFilePath) return
				// 调用现有 ASR endpoint（如果存在）
				try {
					const api = await import('../../utils/api.js')
					const fn = api.transcribeVoice || api.default?.transcribeVoice
					if (typeof fn === 'function') {
						const text = await fn(res.tempFilePath)
						if (text) {
							this.form.content = (this.form.content || '') + (text || '')
							uni.showToast({ title: '识别完成' })
							return
						}
					}
					uni.showToast({ title: '已录音但未配置 ASR', icon: 'none' })
				} catch (_) {
					uni.showToast({ title: 'ASR 调用失败', icon: 'none' })
				}
			},
			async onAiDraft() {
				this.aiLoading = true
				try {
					const api = await import('../../utils/api.js')
					const fn = api.generateDailyReportDraft || api.default?.generateDailyReportDraft
					if (typeof fn === 'function') {
						const draft = await fn({})
						if (typeof draft === 'string' && draft) {
							this.form.content = draft
						} else if (draft && draft.content) {
							this.form.content = draft.content
							this.form.plan = draft.plan || this.form.plan
						}
						uni.showToast({ title: '已生成草稿' })
					} else {
						uni.showToast({ title: '后端 AI 草稿接口未配置', icon: 'none' })
					}
				} catch (e) {
					uni.showToast({ title: '生成失败', icon: 'none' })
				} finally {
					this.aiLoading = false
				}
			},
			async submit() {
				if (!this.form.content) {
					uni.showToast({ title: '请填写日报内容', icon: 'none' })
					return
				}
				this.submitting = true
				try {
					const api = await import('../../utils/api.js')
					const fn = api.submitDailyReport || api.default?.submitDailyReport
					if (typeof fn === 'function') {
						await fn(this.form)
					}
					uni.showToast({ title: '已提交' })
					setTimeout(() => uni.navigateBack(), 800)
				} catch (e) {
					uni.showToast({ title: '提交失败', icon: 'none' })
				} finally {
					this.submitting = false
				}
			},
		},
	}
</script>

<style lang="scss" scoped>
	.page { padding: 24rpx; }

	.card {
		background: #fff;
		border-radius: 16rpx;
		padding: 32rpx;
		display: flex;
		flex-direction: column;
		gap: 32rpx;
	}

	.title { font-size: 36rpx; font-weight: 600; }

	.form-label { font-size: 28rpx; color: #555; display: block; margin-bottom: 12rpx; }

	.form-textarea {
		background: #f5f7fa;
		border-radius: 8rpx;
		padding: 16rpx;
		font-size: 28rpx;
		min-height: 200rpx;
		width: 100%;
		box-sizing: border-box;
	}

	.quick-actions {
		display: flex;
		gap: 16rpx;
		margin-top: 16rpx;
	}

	.quick-btn {
		flex: 1;
		font-size: 26rpx;
		background: #eef3ff;
		color: #4a90e2;
		border: none;
	}

	.action-bar { margin-top: 32rpx; }
</style>
