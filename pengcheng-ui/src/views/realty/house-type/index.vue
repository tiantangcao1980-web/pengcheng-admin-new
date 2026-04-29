<template>
  <div class="house-type-page">
    <!-- 顶部筛选 -->
    <n-card>
      <n-space align="center" wrap>
        <n-form-item label="楼盘" :show-feedback="false">
          <n-select
            v-model:value="selectedProjectId"
            :options="projectOptions"
            label-field="projectName"
            value-field="id"
            placeholder="请选择楼盘"
            filterable
            remote
            @search="handleProjectSearch"
            style="width: 220px"
            @update:value="loadList"
          />
        </n-form-item>
        <n-button type="primary" :disabled="!selectedProjectId" @click="openCreate">新增户型</n-button>
      </n-space>
    </n-card>

    <!-- 列表 -->
    <n-card style="margin-top: 12px">
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(r: HouseType) => r.id"
        striped
      />
    </n-card>

    <!-- 新增/编辑 Modal -->
    <n-modal
      v-model:show="modalVisible"
      :title="editingId ? '编辑户型' : '新增户型'"
      preset="card"
      style="width: 560px"
      @after-leave="resetForm"
    >
      <n-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <n-form-item label="户型代码" path="code">
          <n-input v-model:value="form.code" placeholder="如 A1 / B2" />
        </n-form-item>
        <n-form-item label="户型名称" path="name">
          <n-input v-model:value="form.name" placeholder="如 三室两厅" />
        </n-form-item>
        <n-grid :cols="3" :x-gap="12">
          <n-gi>
            <n-form-item label="卧室">
              <n-input-number v-model:value="form.bedrooms" :min="0" style="width:100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="客厅">
              <n-input-number v-model:value="form.livingRooms" :min="0" style="width:100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="卫生间">
              <n-input-number v-model:value="form.bathrooms" :min="0" style="width:100%" />
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-grid :cols="2" :x-gap="12">
          <n-gi>
            <n-form-item label="建筑面积 m²" path="area">
              <n-input-number v-model:value="form.area" :precision="2" :min="0" style="width:100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="套内面积 m²">
              <n-input-number v-model:value="form.insideArea" :precision="2" :min="0" style="width:100%" />
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-form-item label="朝向">
          <n-select
            v-model:value="form.orientation"
            :options="orientationOptions"
            clearable
            style="width: 100%"
          />
        </n-form-item>
        <n-form-item label="指导价（元）">
          <n-input-number v-model:value="form.basePrice" :precision="2" :min="0" style="width:100%" />
        </n-form-item>
        <n-form-item label="户型图">
          <n-input v-model:value="form.layoutImage" placeholder="粘贴图片 URL" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="form.description" type="textarea" :rows="3" />
        </n-form-item>
        <n-form-item label="是否启用">
          <n-switch v-model:value="enabledBool" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h } from 'vue'
import { NButton, NSpace, NSwitch, NTag, useMessage, type DataTableColumns } from 'naive-ui'
import { houseTypeApi, type HouseType } from '@/api/realtyUnit'
import { request } from '@/utils/request'

const message = useMessage()

// ---------- 状态 ----------
const selectedProjectId = ref<number | null>(null)
const projectOptions = ref<{ id: number; projectName: string }[]>([])
const tableData = ref<HouseType[]>([])
const loading = ref(false)
const modalVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<any>(null)

const form = ref<HouseType>({
  projectId: 0,
  code: '',
  name: '',
  bedrooms: 0,
  livingRooms: 0,
  bathrooms: 0,
  area: 0,
  insideArea: undefined,
  orientation: undefined,
  layoutImage: '',
  basePrice: undefined,
  description: '',
  enabled: 1,
})

const enabledBool = computed({
  get: () => form.value.enabled === 1,
  set: (v: boolean) => { form.value.enabled = v ? 1 : 0 },
})

const orientationOptions = [
  { label: '南', value: '南' },
  { label: '北', value: '北' },
  { label: '东', value: '东' },
  { label: '西', value: '西' },
  { label: '南北', value: '南北' },
  { label: '东南', value: '东南' },
  { label: '西南', value: '西南' },
  { label: '东北', value: '东北' },
  { label: '西北', value: '西北' },
]

const rules = {
  code: [{ required: true, message: '请填写户型代码', trigger: 'blur' }],
  name: [{ required: true, message: '请填写户型名称', trigger: 'blur' }],
  area: [{ required: true, type: 'number', message: '请填写建筑面积', trigger: 'blur' }],
}

// ---------- 表格列 ----------
const columns: DataTableColumns<HouseType> = [
  { title: '代码', key: 'code', width: 80 },
  { title: '名称', key: 'name', width: 120 },
  {
    title: '户型结构', key: 'structure', width: 120,
    render: (row) => `${row.bedrooms}室${row.livingRooms}厅${row.bathrooms}卫`,
  },
  { title: '建筑面积', key: 'area', width: 100, render: (row) => `${row.area} m²` },
  { title: '套内面积', key: 'insideArea', width: 100, render: (row) => row.insideArea ? `${row.insideArea} m²` : '-' },
  { title: '朝向', key: 'orientation', width: 80, render: (row) => row.orientation ?? '-' },
  {
    title: '指导价', key: 'basePrice', width: 120,
    render: (row) => row.basePrice ? `${(row.basePrice / 10000).toFixed(2)} 万` : '-',
  },
  {
    title: '状态', key: 'enabled', width: 80,
    render: (row) => h(NTag, { type: row.enabled === 1 ? 'success' : 'default' }, { default: () => row.enabled === 1 ? '启用' : '停用' }),
  },
  {
    title: '操作', key: 'actions', width: 140,
    render: (row) =>
      h(NSpace, {}, {
        default: () => [
          h(NButton, { size: 'tiny', onClick: () => openEdit(row) }, { default: () => '编辑' }),
          h(NButton, { size: 'tiny', type: 'error', onClick: () => handleDelete(row.id!) }, { default: () => '删除' }),
        ],
      }),
  },
]

// ---------- 操作 ----------
const handleProjectSearch = async (keyword: string) => {
  if (!keyword) return
  try {
    const res = await request.get<any[]>('/admin/project/search', { params: { keyword } })
    projectOptions.value = res as any
  } catch { /* ignore */ }
}

const loadList = async (id: number | null) => {
  if (!id) { tableData.value = []; return }
  loading.value = true
  try {
    tableData.value = await houseTypeApi.listByProject(id) as unknown as HouseType[]
  } catch { message.error('加载户型列表失败') }
  finally { loading.value = false }
}

const openCreate = () => {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

const openEdit = (row: HouseType) => {
  editingId.value = row.id!
  Object.assign(form.value, row)
  modalVisible.value = true
}

const resetForm = () => {
  form.value = {
    projectId: selectedProjectId.value ?? 0,
    code: '', name: '',
    bedrooms: 0, livingRooms: 0, bathrooms: 0,
    area: 0, insideArea: undefined, orientation: undefined,
    layoutImage: '', basePrice: undefined, description: '', enabled: 1,
  }
  editingId.value = null
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    form.value.projectId = selectedProjectId.value!
    if (editingId.value) {
      await houseTypeApi.update(editingId.value, form.value)
      message.success('户型更新成功')
    } else {
      await houseTypeApi.create(form.value)
      message.success('户型创建成功')
    }
    modalVisible.value = false
    await loadList(selectedProjectId.value)
  } catch (e: any) {
    message.error(e?.message ?? '保存失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await houseTypeApi.delete(id)
    message.success('删除成功')
    await loadList(selectedProjectId.value)
  } catch { message.error('删除失败') }
}
</script>

<style scoped>
.house-type-page {
  padding: 16px;
}
</style>
