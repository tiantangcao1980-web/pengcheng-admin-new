<template>
  <view class="copilot-page">
    <view class="copilot-header">
      <text class="copilot-title">AI 助手</text>
      <text class="copilot-subtitle">问我业务问题、写文案、查报表</text>
    </view>

    <scroll-view class="copilot-body" scroll-y :scroll-into-view="lastBubbleId" :scroll-with-animation="true">
      <view v-if="messages.length === 0" class="copilot-empty">
        <text>试试问我：</text>
        <text>· 这个月我跟了几个客户？</text>
        <text>· 今天我要做什么？</text>
        <text>· 帮我写条给王总的回访话术。</text>
      </view>
      <view v-for="m in messages" :id="`m-${m.id}`" :key="m.id">
        <copilot-bubble :msg="m" @confirm="onConfirmAction" @cancel="onCancelAction" />
      </view>
    </scroll-view>

    <view class="copilot-footer">
      <view class="copilot-input-row">
        <input v-model="input" class="copilot-input" placeholder="说点什么…" :disabled="loading" @confirm="onSend" />
        <button class="copilot-mic" size="mini" :disabled="loading" @tap="onMicTap">
          {{ recording ? '松开发送' : '🎤' }}
        </button>
      </view>
      <view class="copilot-action-row">
        <button class="copilot-send" :disabled="!canSend" type="primary" @tap="onSend">
          {{ loading ? '思考中…' : '发送' }}
        </button>
      </view>
    </view>

    <!-- H2：AI 提议动作时弹出二次确认弹窗 -->
    <tool-confirm-popup
      v-if="pendingProposal"
      :proposal="pendingProposal"
      :show="!!pendingProposal"
      @done="onProposalDone"
      @cancel="onProposalCancel"
    />
  </view>
</template>

<script>
/**
 * V4.0 MVP 闭环④ — uniapp Copilot 主页面（不修改既有 chat.vue）。
 *
 * 责任：
 *   - 与全局上下文（用户、当前路由、可用动作）一同发送给后端
 *   - 流式响应：使用 pollChat 长轮询（小程序无原生 SSE）
 *   - 工具建议：弹出确认按钮 → propose/confirm
 *   - 语音输入：调用 uni.startRecord / uni.stopRecord（APP/MP 通用），
 *     结束后上传得到 audioUrl → /admin/ai/asr/transcribe → 文字回填到 input
 */
import CopilotBubble from '../../components/copilot/copilot-bubble.vue'
import ToolConfirmPopup from '../../components/copilot/tool-confirm-popup.vue'
import { aiCopilotApi } from '../../components/copilot/copilotApi.js'

let _idSeq = 0
const newId = () => `${Date.now()}-${++_idSeq}`

export default {
  components: { CopilotBubble, ToolConfirmPopup },
  data() {
    return {
      input: '',
      messages: [],
      loading: false,
      conversationId: null,
      recording: false,
      recorderManager: null,
      /** H2：AI 提议的待确认动作，非 null 时弹出 tool-confirm-popup */
      pendingProposal: null
    }
  },
  computed: {
    canSend() {
      return !!this.input?.trim() && !this.loading
    },
    lastBubbleId() {
      const last = this.messages[this.messages.length - 1]
      return last ? `m-${last.id}` : ''
    }
  },
  mounted() {
    try {
      this.recorderManager = uni.getRecorderManager()
      this.recorderManager.onStop(this.onRecorderStop)
      this.recorderManager.onError(() => {
        this.recording = false
        uni.showToast({ title: '录音失败', icon: 'none' })
      })
    } catch (_) { /* ignore */ }
  },
  methods: {
    onSend() {
      const text = (this.input || '').trim()
      if (!text || this.loading) return
      this.input = ''
      this.messages.push({ id: newId(), role: 'user', content: text })
      const assistant = { id: newId(), role: 'assistant', content: '', streaming: true }
      this.messages.push(assistant)
      this.loading = true

      const ctx = this.collectContext()
      aiCopilotApi.pollChat(
        { message: text, conversationId: this.conversationId, context: ctx },
        (chunk) => {
          assistant.content += chunk
          this.$forceUpdate?.()
        },
        (final) => {
          assistant.streaming = false
          this.loading = false
          if (final?.conversationId) this.conversationId = final.conversationId
          // H2：后端 poll 响应中携带 tool_proposal 时直接填充 pendingProposal
          if (final?.toolProposal) {
            this.pendingProposal = final.toolProposal
          } else if (final?.suggestion) {
            this.proposeAction(assistant, final.suggestion)
          }
        },
        (err) => {
          assistant.streaming = false
          assistant.content += `\n\n_请求失败：${err?.message || err}_`
          this.loading = false
        }
      )
    },

    async proposeAction(target, sug) {
      try {
        const proposal = await aiCopilotApi.proposeAction({
          actionCode: sug.actionCode,
          conversationId: this.conversationId,
          userId: this.collectContext().userId,
          pagePath: this.collectContext().pagePath,
          payload: sug.payload || {}
        })
        target.pendingAction = {
          actionId: proposal.actionId,
          confirmToken: proposal.confirmToken,
          summary: proposal.summary
        }
      } catch (e) {
        target.content += `\n\n_动作提议失败：${e?.message || e}_`
      }
    },

    async onConfirmAction(msg) {
      if (!msg.pendingAction) return
      try {
        const result = await aiCopilotApi.confirmAction(
          msg.pendingAction.actionId,
          msg.pendingAction.confirmToken
        )
        msg.content += `\n\n✅ ${result || '动作已执行'}`
      } catch (e) {
        msg.content += `\n\n❌ 执行失败：${e?.message || e}`
      } finally {
        msg.pendingAction = null
      }
    },

    async onCancelAction(msg) {
      if (!msg.pendingAction) return
      try {
        await aiCopilotApi.cancelAction(msg.pendingAction.actionId, msg.pendingAction.confirmToken)
      } catch (_) { /* ignore */ }
      msg.pendingAction = null
    },

    /** H2：tool-confirm-popup 确认完成（成功或失败均回调） */
    onProposalDone(result) {
      this.messages.push({
        id: `${Date.now()}-${++_idSeq}`,
        role: 'assistant',
        content: `✅ ${result}`
      })
      this.pendingProposal = null
      this.$forceUpdate?.()
    },

    /** H2：用户取消 tool-confirm-popup */
    onProposalCancel() {
      this.pendingProposal = null
    },

    onMicTap() {
      if (!this.recorderManager) {
        uni.showToast({ title: '当前平台不支持录音', icon: 'none' })
        return
      }
      if (!this.recording) {
        this.recording = true
        this.recorderManager.start({ duration: 60000, sampleRate: 16000, format: 'mp3' })
        uni.showToast({ title: '录音中…再次点击结束', icon: 'none' })
      } else {
        this.recording = false
        this.recorderManager.stop()
      }
    },

    async onRecorderStop(res) {
      this.recording = false
      const tempPath = res?.tempFilePath
      if (!tempPath) return
      // TODO(V4.0 D4): APP 端走 plus.uploader 上传到 OSS 拿稳定 URL；
      //  MVP 阶段直接把 tempPath 作为 audioUrl 让后端 mock 转写返回占位文字。
      try {
        const result = await aiCopilotApi.transcribe(tempPath, 'mp3')
        if (result?.transcript) {
          this.input = (this.input || '') + result.transcript
          uni.showToast({ title: '已转写', icon: 'success' })
        }
      } catch (e) {
        uni.showToast({ title: '转写失败', icon: 'none' })
      }
    },

    collectContext() {
      let userId
      let pagePath
      try {
        const user = uni.getStorageSync('userInfo')
        userId = user?.id
      } catch (_) { /* ignore */ }
      try {
        const pages = getCurrentPages()
        pagePath = pages?.[pages.length - 1]?.route
      } catch (_) { /* ignore */ }
      return {
        userId,
        pagePath,
        availableActions: ['FOLLOW_UP_CREATE', 'TODO_CREATE', 'APPROVAL_SUBMIT']
      }
    }
  }
}
</script>

<style scoped>
.copilot-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f3f4f6;
}
.copilot-header {
  padding: 24rpx 32rpx;
  background: #fff;
  border-bottom: 1rpx solid #e5e7eb;
}
.copilot-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #111827;
}
.copilot-subtitle {
  display: block;
  font-size: 24rpx;
  color: #6b7280;
  margin-top: 8rpx;
}
.copilot-body {
  flex: 1;
  padding: 16rpx 24rpx;
}
.copilot-empty {
  padding: 80rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  color: #9ca3af;
  font-size: 26rpx;
  line-height: 1.7;
}
.copilot-footer {
  padding: 16rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid #e5e7eb;
}
.copilot-input-row {
  display: flex;
  gap: 12rpx;
  align-items: center;
}
.copilot-input {
  flex: 1;
  height: 72rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 0 24rpx;
  background: #f9fafb;
  font-size: 28rpx;
}
.copilot-mic {
  width: 96rpx;
}
.copilot-send {
  width: 100%;
  margin-top: 16rpx;
  background: #111827 !important;
  color: #fff !important;
}
.copilot-action-row {
  margin-top: 4rpx;
}
</style>
