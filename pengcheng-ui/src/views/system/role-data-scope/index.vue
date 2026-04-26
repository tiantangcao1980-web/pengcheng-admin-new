<template>
  <div class="role-data-scope">
    <n-page-header title="角色数据权限" subtitle="V4.0 闭环 ① 数据权限四档配置" />

    <n-card style="margin-top: 16px">
      <p class="hint">
        当前 RBAC 数据权限四档：仅本人 / 本部门 / 本部门及下级 / 全部 / 自定义。修改后立即生效，
        命中所有挂了 <code>@DataScope</code> 注解的查询。
      </p>
      <n-data-table :columns="columns" :data="rows" :loading="loading" :pagination="{ pageSize: 20 }" />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted } from 'vue'
import {
  NPageHeader,
  NCard,
  NDataTable,
  NSelect,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import { roleApi, type SysRole } from '@/api/system'
import { roleDataScopeApi, type DataScopeOption } from '@/api/tenant'

const message = useMessage()

const rows = ref<SysRole[]>([])
const loading = ref(false)
const options = ref<DataScopeOption[]>([])

async function load() {
  loading.value = true
  try {
    const [list, opts] = await Promise.all([
      roleApi.page({ page: 1, pageSize: 100 }),
      roleDataScopeApi.options()
    ])
    rows.value = (list as any).rows ?? (list as any).data ?? []
    options.value = opts
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function updateScope(role: SysRole, dataScope: number) {
  try {
    await roleDataScopeApi.update(role.id, dataScope)
    role.dataScope = dataScope
    message.success(`已更新角色 ${role.name} 的数据范围`)
  } catch (e: any) {
    message.error(e?.message || '更新失败')
  }
}

const columns: DataTableColumns<SysRole> = [
  { key: 'name', title: '角色名称' },
  { key: 'code', title: '角色编码' },
  {
    key: 'dataScope',
    title: '数据范围',
    width: 240,
    render: (row) =>
      h(NSelect, {
        value: row.dataScope,
        options: options.value,
        size: 'small',
        style: 'width: 200px',
        onUpdateValue: (v: number) => updateScope(row, v)
      })
  }
]

onMounted(load)
</script>

<style scoped>
.role-data-scope {
  padding: 16px;
}
.hint {
  color: #6b7280;
}
.hint code {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
