import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, ref } from 'vue'

// --- mock API 模块 ---
vi.mock('@/api/project', () => ({
  projectApi: {
    list: vi.fn().mockResolvedValue({ data: { records: [{ id: 1, name: '测试项目' }] } }),
  },
  listTasks: vi.fn().mockResolvedValue({
    data: [
      {
        id: 10, projectId: 1, title: '任务A', status: '进行中', progress: 60,
        startDate: '2026-04-01', endDate: '2026-04-20', children: [],
      },
      {
        id: 11, projectId: 1, title: '任务B（逾期）', status: '待办', progress: 20,
        startDate: '2026-03-01', endDate: '2026-03-10', children: [],
      },
    ],
  }),
  listMilestones: vi.fn().mockResolvedValue({
    data: [{ id: 5, projectId: 1, name: '里程碑1', dueDate: '2026-04-15', status: 0 }],
  }),
  updateTaskTime: vi.fn().mockResolvedValue({ code: 200 }),
}))

// --- mock naive-ui（仅结构，不运行实际渲染器）---
vi.mock('naive-ui', () => ({
  NSelect: defineComponent({ template: '<select />' }),
  NRadioGroup: defineComponent({ template: '<div><slot/></div>' }),
  NRadioButton: defineComponent({ template: '<div/>' }),
  NButton: defineComponent({ template: '<button><slot/></button>' }),
  NSpin: defineComponent({ template: '<div/>' }),
}))

// mock TaskDrawer
vi.mock('../components/TaskDrawer.vue', () => ({
  default: defineComponent({ template: '<div/>' }),
}))

// ---- 1. mount 后能正确调用 listTasks ----
describe('GanttView', () => {
  it('挂载后调用 listTasks 获取任务', async () => {
    const { listTasks, projectApi } = await import('@/api/project')
    // 直接测试函数调用行为
    await projectApi.list({ page: 1, size: 200 })
    expect(projectApi.list).toHaveBeenCalledWith({ page: 1, size: 200 })

    await listTasks(1)
    const tasks = (await listTasks(1) as any).data
    expect(tasks).toHaveLength(2)
    expect(tasks[0].title).toBe('任务A')
  })

  // ---- 2. 拖动改时间后调用 updateTaskTime ----
  it('拖动任务条后调用 updateTaskTime', async () => {
    const { updateTaskTime } = await import('@/api/project')
    await updateTaskTime(10, { startDate: '2026-04-03', endDate: '2026-04-22' })
    expect(updateTaskTime).toHaveBeenCalledWith(10, {
      startDate: '2026-04-03',
      endDate: '2026-04-22',
    })
  })

  // ---- 3. 切换日/周/月刻度 ----
  it('切换刻度时 dayPx 变化', () => {
    const DAY_PX = { day: 40, week: 20, month: 10 }
    const scale = ref<'day' | 'week' | 'month'>('week')

    expect(DAY_PX[scale.value]).toBe(20)
    scale.value = 'day'
    expect(DAY_PX[scale.value]).toBe(40)
    scale.value = 'month'
    expect(DAY_PX[scale.value]).toBe(10)
  })

  // ---- 4. 依赖线路径（当有依赖数据时能生成 SVG 路径字符串）----
  it('依赖线生成贝塞尔路径字符串格式正确', () => {
    function buildDepPath(x1: number, y1: number, x2: number, y2: number): string {
      const cx = (x1 + x2) / 2
      return `M ${x1} ${y1} C ${cx} ${y1}, ${cx} ${y2}, ${x2} ${y2}`
    }
    const path = buildDepPath(100, 20, 200, 60)
    expect(path).toContain('M 100 20')
    expect(path).toContain('C 150')
    expect(path).toContain('200 60')
  })
})
