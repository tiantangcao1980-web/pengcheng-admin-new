/**
 * V4.0 MVP H2 — ToolConfirmDialog 单测。
 *
 * 覆盖 3 个用例：
 *   1. 渲染：展示动作标签、summary、payload 预览字段
 *   2. 点击"确认执行"→ 调用 aiCopilotApi.executeAction，emit('done', result)
 *   3. 点击"取消"→ emit('cancel')，不调用 API
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// --- mock Naive UI 最小桩 ---
vi.mock('naive-ui', () => {
  const { defineComponent, h, ref, computed } = require('vue')

  const NModal = defineComponent({
    props: { show: Boolean, title: String, maskClosable: Boolean },
    emits: ['afterLeave'],
    setup(props, { slots }) {
      return () =>
        props.show
          ? h('div', { 'data-testid': 'n-modal' }, [
              h('div', { class: 'n-modal-title' }, props.title),
              slots.default?.(),
              slots.footer?.()
            ])
          : null
    }
  })

  const NButton = defineComponent({
    props: { disabled: Boolean, loading: Boolean, type: String },
    emits: ['click'],
    setup(props, { slots, emit }) {
      return () =>
        h('button', { disabled: props.disabled || props.loading, onClick: () => emit('click') }, slots.default?.())
    }
  })

  const NTag = defineComponent({
    props: { type: String, size: String },
    setup(_, { slots }) {
      return () => h('span', { class: 'n-tag' }, slots.default?.())
    }
  })

  const NDescriptions = defineComponent({
    props: { column: Number, labelPlacement: String, bordered: Boolean, size: String },
    setup(_, { slots }) {
      return () => h('dl', null, slots.default?.())
    }
  })

  const NDescriptionsItem = defineComponent({
    props: { label: String },
    setup(props, { slots }) {
      return () => h('div', null, [h('dt', null, props.label), h('dd', null, slots.default?.())])
    }
  })

  return { NModal, NButton, NTag, NDescriptions, NDescriptionsItem }
})

// --- mock aiCopilotApi ---
vi.mock('@/api/aiCopilot', () => ({
  aiCopilotApi: {
    executeAction: vi.fn()
  }
}))

import ToolConfirmDialog, { type ToolProposal } from '../ToolConfirmDialog.vue'
import { aiCopilotApi } from '@/api/aiCopilot'

const baseProposal: ToolProposal = {
  action: 'FOLLOW_UP_CREATE',
  summary: '新建客户跟进（客户：王总）',
  payload: { customerName: '王总', note: '上午来电咨询学区房', salesName: '李明' },
  confirmToken: 'test-token-abc',
  actionId: 101
}

describe('ToolConfirmDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ------------------------------------------------------------------
  // 用例 1：渲染正确展示摘要和 payload 预览字段
  // ------------------------------------------------------------------
  it('渲染动作名称、summary 和 payload 关键字段', async () => {
    const wrapper = mount(ToolConfirmDialog, {
      props: { proposal: baseProposal }
    })
    await nextTick()

    const html = wrapper.html()
    // 动作标签
    expect(html).toContain('新建跟进')
    // 摘要
    expect(html).toContain('新建客户跟进（客户：王总）')
    // payload 预览字段
    expect(html).toContain('王总')
    expect(html).toContain('上午来电咨询学区房')
    expect(html).toContain('李明')
  })

  // ------------------------------------------------------------------
  // 用例 2：点"确认执行"调 executeAction，emit('done', result)
  // ------------------------------------------------------------------
  it('点击确认执行时调用 executeAction 并 emit done', async () => {
    vi.mocked(aiCopilotApi.executeAction).mockResolvedValueOnce('已为客户王总新建跟进 #1001')

    const wrapper = mount(ToolConfirmDialog, {
      props: { proposal: baseProposal }
    })
    await nextTick()

    // 找到"确认执行"按钮并点击
    const confirmBtn = wrapper.findAll('button').find((b) => b.text().includes('确认执行'))
    expect(confirmBtn).toBeDefined()
    await confirmBtn!.trigger('click')
    await nextTick()
    // 等待异步 Promise
    await new Promise((r) => setTimeout(r, 0))

    expect(aiCopilotApi.executeAction).toHaveBeenCalledWith({ token: 'test-token-abc' })
    expect(wrapper.emitted('done')).toBeTruthy()
    expect(wrapper.emitted('done')![0]).toEqual(['已为客户王总新建跟进 #1001'])
  })

  // ------------------------------------------------------------------
  // 用例 3：点"取消" emit cancel，不调用 API
  // ------------------------------------------------------------------
  it('点击取消时 emit cancel 且不调用 executeAction', async () => {
    const wrapper = mount(ToolConfirmDialog, {
      props: { proposal: baseProposal }
    })
    await nextTick()

    const cancelBtn = wrapper.findAll('button').find((b) => b.text() === '取消')
    expect(cancelBtn).toBeDefined()
    await cancelBtn!.trigger('click')
    await nextTick()

    expect(aiCopilotApi.executeAction).not.toHaveBeenCalled()
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })
})
