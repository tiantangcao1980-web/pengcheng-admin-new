/**
 * V4.0 MVP 闭环④ — uniapp 端 Copilot API 封装。
 * 与 Web 端 src/api/aiCopilot.ts 一一对应，但使用 uni.request 而非 fetch。
 */
import { request } from '../../utils/request.js'

export const aiCopilotApi = {
  chat(req) {
    return request({
      url: '/api/admin/ai/copilot/chat',
      method: 'POST',
      data: req
    })
  },

  /**
   * uni-app 暂不支持原生 SSE 流式（小程序不支持 ReadableStream），
   * 这里降级为长轮询：每 700ms 调用一次 chat/poll，直到 done=true。
   * APP 端如需真正流式，可换 plus.net 或 socket。
   */
  async pollChat(req, onChunk, onDone, onError) {
    let cursor = 0
    let stopped = false
    const tick = async () => {
      if (stopped) return
      try {
        const resp = await request({
          url: '/api/admin/ai/copilot/chat/poll',
          method: 'POST',
          data: { ...req, cursor }
        })
        if (resp?.delta) {
          onChunk?.(resp.delta)
          cursor = resp.cursor ?? cursor + (resp.delta?.length || 0)
        }
        if (resp?.done) {
          stopped = true
          onDone?.(resp)
        } else if (!stopped) {
          setTimeout(tick, 700)
        }
      } catch (e) {
        stopped = true
        onError?.(e)
      }
    }
    tick()
    return () => { stopped = true }
  },

  proposeAction(req) {
    return request({
      url: '/api/admin/ai/copilot/actions/propose',
      method: 'POST',
      data: req
    })
  },

  confirmAction(actionId, confirmToken) {
    return request({
      url: '/api/admin/ai/copilot/actions/confirm',
      method: 'POST',
      data: { actionId, confirmToken }
    })
  },

  cancelAction(actionId, confirmToken) {
    return request({
      url: '/api/admin/ai/copilot/actions/cancel',
      method: 'POST',
      data: { actionId, confirmToken }
    })
  },

  /**
   * H2：用户二次确认后执行动作（token-only 简化接口）。
   * POST /api/admin/ai/copilot/action/execute
   *
   * @param {string} token confirmToken
   * @returns {Promise<string>} 执行结果摘要
   */
  executeAction(token) {
    return request({
      url: '/api/admin/ai/copilot/action/execute',
      method: 'POST',
      data: { token }
    })
  },

  transcribe(audioUrl, format = 'mp3') {
    return request({
      url: '/api/admin/ai/asr/transcribe',
      method: 'POST',
      data: { audioUrl, format }
    })
  }
}

export default aiCopilotApi
