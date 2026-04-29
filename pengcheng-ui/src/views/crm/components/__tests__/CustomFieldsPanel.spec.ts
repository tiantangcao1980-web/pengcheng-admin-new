import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import CustomFieldsPanel from '../CustomFieldsPanel.vue'

// --- Mock customFieldApi ---
const mockListDefs = vi.fn()
const mockLoadValues = vi.fn()
const mockSaveValues = vi.fn()

vi.mock('@/api/customField', () => ({
  customFieldApi: {
    listDefs: (...a: any[]) => mockListDefs(...a),
    loadValues: (...a: any[]) => mockLoadValues(...a),
    saveValues: (...a: any[]) => mockSaveValues(...a)
  }
}))

// --- Mock DynamicFieldRenderer ---
vi.mock('@/views/crm/custom-field/DynamicFieldRenderer.vue', () => ({
  default: {
    name: 'DynamicFieldRenderer',
    props: ['defs', 'modelValue'],
    emits: ['update:modelValue'],
    template: '<div class="mock-renderer" data-testid="renderer"></div>'
  }
}))

const ALL_TYPES_DEFS = [
  { id: 1, fieldKey: 'f_text', label: '文本', fieldType: 'text', required: 0 },
  { id: 2, fieldKey: 'f_number', label: '数字', fieldType: 'number', required: 0 },
  { id: 3, fieldKey: 'f_date', label: '日期', fieldType: 'date', required: 0 },
  { id: 4, fieldKey: 'f_select', label: '单选', fieldType: 'select', required: 0, optionsJson: '[{"value":"a","label":"A"}]' },
  { id: 5, fieldKey: 'f_multi', label: '多选', fieldType: 'multi_select', required: 0, optionsJson: '[{"value":"x","label":"X"}]' },
  { id: 6, fieldKey: 'f_file', label: '文件', fieldType: 'file', required: 0 }
]

describe('CustomFieldsPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockListDefs.mockResolvedValue({ data: [] })
    mockLoadValues.mockResolvedValue({ data: {} })
    mockSaveValues.mockResolvedValue({ data: null })
  })

  it('mount 时显示 loading，完成后渲染字段', async () => {
    mockListDefs.mockResolvedValue({ data: ALL_TYPES_DEFS })
    mockLoadValues.mockResolvedValue({ data: { f_text: 'hello' } })

    const wrapper = mount(CustomFieldsPanel, {
      props: { entityType: 'lead', entityId: 1 }
    })

    // 挂载后立即应处于 loading
    expect(wrapper.find('.cfp__loading').exists()).toBe(true)

    await flushPromises()

    // loading 消失，渲染器出现
    expect(wrapper.find('.cfp__loading').exists()).toBe(false)
    expect(wrapper.find('[data-testid="renderer"]').exists()).toBe(true)
    expect(mockListDefs).toHaveBeenCalledWith('lead')
    expect(mockLoadValues).toHaveBeenCalledWith('lead', 1)
  })

  it('渲染 6 种字段类型时，DynamicFieldRenderer 接收到完整 defs', async () => {
    mockListDefs.mockResolvedValue({ data: ALL_TYPES_DEFS })
    mockLoadValues.mockResolvedValue({ data: {} })

    const wrapper = mount(CustomFieldsPanel, {
      props: { entityType: 'customer', entityId: 42 }
    })
    await flushPromises()

    // DynamicFieldRenderer 已挂载，且 defs 包含 6 项
    const renderer = wrapper.findComponent({ name: 'DynamicFieldRenderer' })
    expect(renderer.exists()).toBe(true)
    expect((renderer.props('defs') as any[]).length).toBe(6)

    const types = (renderer.props('defs') as any[]).map((d: any) => d.fieldType)
    expect(types).toContain('text')
    expect(types).toContain('number')
    expect(types).toContain('date')
    expect(types).toContain('select')
    expect(types).toContain('multi_select')
    expect(types).toContain('file')
  })

  it('readonly=true 时隐藏保存按钮', async () => {
    mockListDefs.mockResolvedValue({ data: ALL_TYPES_DEFS })
    mockLoadValues.mockResolvedValue({ data: {} })

    const wrapper = mount(CustomFieldsPanel, {
      props: { entityType: 'lead', entityId: 1, readonly: true }
    })
    await flushPromises()

    expect(wrapper.find('.cfp__save-btn').exists()).toBe(false)
  })

  it('点击保存按钮调用 saveValues API', async () => {
    mockListDefs.mockResolvedValue({ data: ALL_TYPES_DEFS })
    mockLoadValues.mockResolvedValue({ data: { f_text: 'v' } })

    const wrapper = mount(CustomFieldsPanel, {
      props: { entityType: 'lead', entityId: 7 }
    })
    await flushPromises()

    const saveBtn = wrapper.find('.cfp__save-btn')
    expect(saveBtn.exists()).toBe(true)

    await saveBtn.trigger('click')
    await flushPromises()

    expect(mockSaveValues).toHaveBeenCalledWith('lead', 7, expect.any(Object))
  })
})
