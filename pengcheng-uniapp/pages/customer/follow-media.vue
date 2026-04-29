<template>
	<view class="page">
		<view class="card">
			<text class="title">多媒体跟进</text>
			<view class="row"><text class="lbl">跟进 ID</text><input v-model.number="visitId" type="number" /></view>

			<view class="row"><text class="lbl">媒体类型</text>
				<picker :range="types" @change="e => mediaTypeIdx = e.detail.value">
					<text>{{ types[mediaTypeIdx] }}</text>
				</picker>
			</view>

			<view class="row"><text class="lbl">语音时长(秒)</text><input v-model.number="voiceDuration" type="number" /></view>
			<view class="row"><text class="lbl">备注</text><input v-model="remark" /></view>
		</view>

		<view class="card">
			<text class="title">采集媒体</text>
			<view class="actions">
				<button @tap="recordAudio">{{ audioRecording ? '停止录音' : '开始录音' }}</button>
				<button @tap="chooseImage">拍照/相册</button>
				<button @tap="chooseVideo">录像</button>
			</view>
			<view class="files">
				<view v-for="(u, i) in mediaUrls" :key="i" class="file-item">{{ u }}</view>
			</view>
		</view>

		<button class="btn primary" @tap="submit" :disabled="busy">提交</button>
	</view>
</template>

<script>
import { put, upload } from '@/utils/request.js'

export default {
	data() {
		return {
			visitId: null,
			types: ['text', 'image', 'audio', 'video', 'mixed'],
			mediaTypeIdx: 1,
			voiceDuration: 0,
			remark: '',
			mediaUrls: [],
			audioRecording: false,
			recorder: null,
			busy: false
		}
	},
	onLoad(options) {
		this.visitId = Number(options.visitId || 0)
	},
	methods: {
		recordAudio() {
			// uni.getRecorderManager() 用于真机录音；H5 需走 mediaRecorder API。
			if (!this.recorder) {
				this.recorder = uni.getRecorderManager()
				this.recorder.onStop((res) => {
					this.audioRecording = false
					this.uploadFile(res.tempFilePath)
				})
			}
			if (this.audioRecording) {
				this.recorder.stop()
			} else {
				this.recorder.start({ duration: 60000, format: 'mp3' })
				this.audioRecording = true
				this.mediaTypeIdx = 2
			}
		},
		chooseImage() {
			uni.chooseImage({
				count: 9,
				success: (res) => {
					this.mediaTypeIdx = 1
					res.tempFilePaths.forEach(p => this.uploadFile(p))
				}
			})
		},
		chooseVideo() {
			uni.chooseVideo({
				success: (res) => {
					this.mediaTypeIdx = 3
					this.uploadFile(res.tempFilePath)
				}
			})
		},
		async uploadFile(path) {
			// 复用 utils/request.js 的 upload 上传到 /api/files（后端 MinIO 直传层）
			try {
				const res = await upload('/files/upload', path, 'file')
				const url = res?.data?.url ?? res?.url
				if (url) this.mediaUrls.push(url)
			} catch (e) {
				uni.showToast({ title: '上传失败', icon: 'none' })
			}
		},
		async submit() {
			if (!this.visitId) {
				uni.showToast({ title: '请填写跟进ID', icon: 'none' })
				return
			}
			this.busy = true
			try {
				await put('/crm/visit-media', {
					visitId: this.visitId,
					mediaType: this.types[this.mediaTypeIdx],
					mediaUrls: this.mediaUrls,
					voiceDuration: this.voiceDuration,
					remark: this.remark
				})
				uni.showToast({ title: '已保存' })
			} finally {
				this.busy = false
			}
		}
	}
}
</script>

<style scoped>
.page { padding: 12px; background: #f5f5f5; min-height: 100vh; }
.card { background: #fff; padding: 12px; border-radius: 6px; margin-bottom: 12px; }
.title { font-weight: 500; display: block; margin-bottom: 8px; }
.row { display: flex; padding: 8px 0; border-bottom: 1px dashed #eee; }
.lbl { width: 96px; color: #999; }
input { flex: 1; }
.actions { display: flex; gap: 8px; }
.actions button { flex: 1; padding: 6px 0; }
.files { margin-top: 8px; }
.file-item { font-size: 12px; color: #555; padding: 4px 0; }
.btn { width: 100%; padding: 10px; border-radius: 4px; }
.btn.primary { background: #1677FF; color: #fff; }
</style>
