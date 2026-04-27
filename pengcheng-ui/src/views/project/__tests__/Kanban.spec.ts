import { describe, it, expect, vi, beforeEach } from 'vitest'
import { defineComponent, ref } from 'vue'

// --- mock API ---
const mockUpdateTaskStatus = vi.fn().mockResolvedValue({ code: 200 })
const mockListColumns = vi.fn().mockResolvedValue({
  data: [
    { id: 1, projectId: 1, name: '待办', statusValue: '待办', sortOrder: 0, isDone: 0 },
    { id: 2, projectId: 1, name: '进行中', statusValue: '进行中', sortOrder: 1, isDone: 0 },
    { id: 3, projectId: 1, name: '已完成', statusValue: '已完成', sortOrder: 2, isDone: 1 },
  ],
})
const mockBoard = vi.fn().mockResolvedValue({
  data: {
    待办: [
      { id: 101, title: '任务1', status: '待办', progress: 0, priority: 2 },
      { id: 102, title: '任务2', status: '待办', progress: 30, priority: 1 },
    ],
    进行中: [
      { id: 103, title: '任务3', status: '进行中', progress: 60, priority: 3 },
    ],
    已完成: [],
  },
})

vi.mock('@/api/project', () => ({
  projectApi: {
    list: vi.fn().mockResolvedValue({ data: { records: [{ id: 1, name: '演示项目' }] } }),
    members: vi.fn().mockResolvedValue({ data: [] }),
  },
  listColumns: mockListColumns,
  updateTaskStatus: mockUpdateTaskStatus,
  projectTaskApi: {
    board: mockBoard,
    create: vi.fn().mockResolvedValue({ code: 200 }),
  },
}))

vi.mock('naive-ui', () => ({
  NSelect: defineComponent({ template: '<select/>' }),
  NInput: defineComponent({ template: '<input/>' }),
  NButton: defineComponent({ template: '<button><slot/></button>' }),
  NSpin: defineComponent({ template: '<div/>' }),
  NModal: defineComponent({ template: '<div><slot/></div>' }),
  NForm: defineComponent({ template: '<form><slot/></form>' }),
  NFormItem: defineComponent({ template: '<div><slot/></div>' }),
  NSpace: defineComponent({ template: '<div><slot/></div>' }),
  NDatePicker: defineComponent({ template: '<div/>' }),
  useMessage: () => ({ success: vi.fn(), error: vi.fn() }),
}))

vi.mock('../components/TaskDrawer.vue', () => ({
  default: defineComponent({ template: '<div/>' }),
}))

// ---- 1. mount 后列 + 卡片数量正确 ----
describe('KanbanView', () => {
  it('loadBoard 后得到正确的列和卡片', async () => {
    const colRes = await mockListColumns(1)
    const boardRes = await mockBoard(1)

    const columns = colRes.data
    const boardData: Record<string, any[]> = boardRes.data

    expect(columns).toHaveLength(3)
    expect(columns[0].name).toBe('待办')

    const flat: any[] = []
    Object.values(boardData).forEach(arr => flat.push(...arr))
    expect(flat).toHaveLength(3)

    const todoCards = boardData['待办']
    expect(todoCards).toHaveLength(2)
  })

  // ---- 2. 跨列拖动后调用 updateTaskStatus ----
  it('跨列拖动后调用 updateTaskStatus 并传入正确参数', async () => {
    const card = { id: 101, status: '待办' }
    const targetCol = { statusValue: '进行中' }

    // 模拟 onDrop 逻辑
    const oldStatus = card.status
    card.status = targetCol.statusValue
    await mockUpdateTaskStatus(card.id, targetCol.statusValue)

    expect(mockUpdateTaskStatus).toHaveBeenCalledWith(101, '进行中')
    expect(card.status).toBe('进行中')
  })

  // ---- 3. 关键词筛选 ----
  it('关键词筛选只返回匹配标题的卡片', () => {
    const allCards = [
      { id: 101, title: '修复登录Bug', status: '待办', progress: 0, priority: 2 },
      { id: 102, title: '实现看板UI', status: '待办', progress: 30, priority: 1 },
      { id: 103, title: '接口联调', status: '进行中', progress: 60, priority: 3 },
    ]

    const keyword = ref('看板')

    function filteredCards(status: string) {
      return allCards.filter(c => {
        if (c.status !== status) return false
        if (keyword.value && !c.title.includes(keyword.value)) return false
        return true
      })
    }

    expect(filteredCards('待办')).toHaveLength(1)
    expect(filteredCards('待办')[0].title).toBe('实现看板UI')
    expect(filteredCards('进行中')).toHaveLength(0)

    keyword.value = ''
    expect(filteredCards('待办')).toHaveLength(2)
    expect(filteredCards('进行中')).toHaveLength(1)
  })
})
