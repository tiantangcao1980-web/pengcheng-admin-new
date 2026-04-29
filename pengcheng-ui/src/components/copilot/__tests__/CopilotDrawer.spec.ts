/**
 * V4.0 MVP 闭环④ Copilot Drawer 流式消息渲染测试。
 *
 * 覆盖：
 *   - StreamRenderer 渲染 Markdown 文本
 *   - streaming=true 时显示打字光标
 *   - copilotState 推入消息后，Drawer 内 bubble 数量随之增加
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

import StreamRenderer from '../StreamRenderer.vue'
import MessageBubble from '../MessageBubble.vue'
import {
  copilotState,
  pushMessage,
  resetCopilot,
  openCopilot
} from '../CopilotStore'

describe('Copilot StreamRenderer', () => {
  it('renders markdown bold to <strong>', async () => {
    const wrapper = mount(StreamRenderer, {
      props: { text: 'hello **world**', streaming: false }
    })
    await nextTick()
    expect(wrapper.html()).toContain('<strong>world</strong>')
  })

  it('shows blinking cursor while streaming', async () => {
    const wrapper = mount(StreamRenderer, {
      props: { text: '加载中', streaming: true }
    })
    await nextTick()
    expect(wrapper.find('[data-testid="copilot-cursor"]').exists()).toBe(true)
  })

  it('appends newly received chunks reactively', async () => {
    const wrapper = mount(StreamRenderer, {
      props: { text: 'a', streaming: true }
    })
    await nextTick()
    expect(wrapper.text()).toContain('a')
    await wrapper.setProps({ text: 'a-b-c' })
    await nextTick()
    expect(wrapper.text()).toContain('a-b-c')
  })
})

describe('Copilot MessageBubble + Store', () => {
  beforeEach(() => {
    resetCopilot()
  })

  it('renders user / assistant bubbles via copilotState.messages', async () => {
    openCopilot()
    pushMessage({ role: 'user', content: '今天的待跟进客户' })
    const assistant = pushMessage({ role: 'assistant', content: '', streaming: true })

    const userBubble = mount(MessageBubble, {
      props: { msg: copilotState.messages[0] }
    })
    expect(userBubble.text()).toContain('今天的待跟进客户')

    // 模拟流式追加
    assistant.content = '您今日有 3 位待跟进客户：'
    const assistantBubble = mount(MessageBubble, {
      props: { msg: assistant }
    })
    await nextTick()
    expect(assistantBubble.text()).toContain('您今日有 3 位待跟进客户')
    expect(assistantBubble.find('[data-testid="copilot-cursor"]').exists()).toBe(true)
  })

  it('shows action confirm panel when pendingAction set', async () => {
    const m = pushMessage({
      role: 'assistant',
      content: '建议新建跟进',
      pendingAction: {
        actionId: 1,
        confirmToken: 'tk-xx',
        summary: '新建客户跟进（客户：王总）'
      }
    })
    const wrapper = mount(MessageBubble, { props: { msg: m } })
    expect(wrapper.find('[data-testid="copilot-action-confirm"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('王总')
    // 确认按钮触发事件
    await wrapper.findAll('button').filter((b) => b.text().includes('确认执行'))[0].trigger('click')
    expect(wrapper.emitted('confirm')).toBeTruthy()
  })
})
