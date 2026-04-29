<template>
  <view :class="['copilot-bubble', `copilot-bubble-${msg.role}`]">
    <view class="copilot-bubble-avatar">
      <text>{{ avatarText }}</text>
    </view>
    <view class="copilot-bubble-content">
      <text class="copilot-bubble-text">{{ msg.content }}</text>
      <text v-if="msg.streaming" class="copilot-cursor">▍</text>

      <view v-if="msg.pendingAction" class="copilot-bubble-action">
        <view class="copilot-action-summary">
          <text>建议执行：{{ msg.pendingAction.summary }}</text>
        </view>
        <view class="copilot-action-buttons">
          <button class="copilot-btn copilot-btn-primary" size="mini" @tap="$emit('confirm', msg)">确认执行</button>
          <button class="copilot-btn" size="mini" @tap="$emit('cancel', msg)">取消</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'CopilotBubble',
  props: {
    msg: { type: Object, required: true }
  },
  computed: {
    avatarText() {
      if (this.msg.role === 'user') return '我'
      if (this.msg.role === 'assistant') return 'AI'
      return '系'
    }
  }
}
</script>

<style scoped>
.copilot-bubble {
  display: flex;
  margin: 16rpx 0;
  align-items: flex-start;
}
.copilot-bubble-user {
  flex-direction: row-reverse;
}
.copilot-bubble-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 16rpx;
}
.copilot-bubble-user .copilot-bubble-avatar {
  background: #111827;
}
.copilot-bubble-user .copilot-bubble-avatar text {
  color: #fff;
}
.copilot-bubble-content {
  background: #f9fafb;
  border-radius: 24rpx;
  padding: 20rpx 24rpx;
  max-width: 70%;
}
.copilot-bubble-user .copilot-bubble-content {
  background: #111827;
}
.copilot-bubble-user .copilot-bubble-text {
  color: #fff;
}
.copilot-bubble-text {
  font-size: 28rpx;
  line-height: 1.6;
  color: #1f2937;
  word-break: break-all;
}
.copilot-cursor {
  display: inline-block;
  margin-left: 4rpx;
  color: #6b7280;
}
.copilot-bubble-action {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx dashed #e5e7eb;
}
.copilot-action-summary text {
  font-size: 24rpx;
  color: #6b7280;
}
.copilot-action-buttons {
  display: flex;
  gap: 12rpx;
  margin-top: 12rpx;
}
.copilot-btn {
  font-size: 24rpx;
}
.copilot-btn-primary {
  background: #111827 !important;
  color: #fff !important;
}
</style>
