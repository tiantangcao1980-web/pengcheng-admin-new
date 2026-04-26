<template>
  <div class="tenant-register">
    <div class="hero">
      <h1>企业一分钟开通</h1>
      <p>三步即用：填企业信息 → 创建管理员 → 立即登录后台</p>
    </div>

    <n-card class="form-card" title="V4.0 企业注册向导">
      <n-steps :current="(step as 1 | 2 | 3)" size="small">
        <n-step title="企业信息" />
        <n-step title="管理员账号" />
        <n-step title="确认提交" />
      </n-steps>

      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="left"
        label-width="120"
        style="margin-top: 24px"
      >
        <template v-if="step === 1">
          <n-form-item path="tenantName" label="企业名称">
            <n-input v-model:value="form.tenantName" placeholder="如：彭程信息科技有限公司" />
          </n-form-item>
          <n-form-item path="industry" label="行业">
            <n-select
              v-model:value="form.industry"
              :options="industryOptions"
              placeholder="请选择行业"
            />
          </n-form-item>
          <n-form-item path="scale" label="规模">
            <n-select
              v-model:value="form.scale"
              :options="scaleOptions"
              placeholder="请选择规模"
            />
          </n-form-item>
        </template>

        <template v-if="step === 2">
          <n-form-item path="adminUsername" label="管理员用户名">
            <n-input
              v-model:value="form.adminUsername"
              placeholder="4-20 位字母/数字/下划线"
              :maxlength="20"
            />
          </n-form-item>
          <n-form-item path="adminPassword" label="管理员密码">
            <n-input
              v-model:value="form.adminPassword"
              type="password"
              placeholder="不少于 6 位"
              show-password-on="click"
              :maxlength="32"
            />
          </n-form-item>
          <n-form-item path="adminPhone" label="管理员手机">
            <n-input v-model:value="form.adminPhone" placeholder="11 位手机号" :maxlength="11" />
          </n-form-item>
          <n-form-item path="adminEmail" label="邮箱">
            <n-input v-model:value="form.adminEmail" placeholder="可选" />
          </n-form-item>
        </template>

        <template v-if="step === 3">
          <n-descriptions :column="1" size="medium" bordered>
            <n-descriptions-item label="企业名称">{{ form.tenantName }}</n-descriptions-item>
            <n-descriptions-item label="行业">{{ form.industry || '—' }}</n-descriptions-item>
            <n-descriptions-item label="规模">{{ form.scale || '—' }}</n-descriptions-item>
            <n-descriptions-item label="管理员">{{ form.adminUsername }}</n-descriptions-item>
            <n-descriptions-item label="手机">{{ form.adminPhone || '—' }}</n-descriptions-item>
            <n-descriptions-item label="邮箱">{{ form.adminEmail || '—' }}</n-descriptions-item>
          </n-descriptions>
        </template>
      </n-form>

      <n-space justify="end" style="margin-top: 24px">
        <n-button v-if="step > 1" @click="step--">上一步</n-button>
        <n-button v-if="step < 3" type="primary" @click="next">下一步</n-button>
        <n-button v-else type="primary" :loading="submitting" @click="submit">提交注册</n-button>
      </n-space>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NSteps,
  NStep,
  NSpace,
  NButton,
  NDescriptions,
  NDescriptionsItem,
  useMessage,
  type FormInst
} from 'naive-ui'
import { tenantApi, type TenantRegisterParams } from '@/api/tenant'

const router = useRouter()
const message = useMessage()

const step = ref(1)
const submitting = ref(false)
const formRef = ref<FormInst | null>(null)

const form = reactive<TenantRegisterParams>({
  tenantName: '',
  industry: '',
  scale: '',
  adminUsername: '',
  adminPassword: '',
  adminPhone: '',
  adminEmail: ''
})

const industryOptions = [
  { label: '房产', value: 'realty' },
  { label: '互联网', value: 'internet' },
  { label: '制造', value: 'manufacturing' },
  { label: '零售', value: 'retail' },
  { label: '教育', value: 'education' },
  { label: '其他', value: 'other' }
]

const scaleOptions = [
  { label: '1-50', value: '1-50' },
  { label: '51-200', value: '51-200' },
  { label: '201-1000', value: '201-1000' },
  { label: '1000+', value: '1000+' }
]

const rules = {
  tenantName: [{ required: true, message: '请输入企业名称', trigger: ['blur'] }],
  adminUsername: [
    { required: true, message: '请输入用户名', trigger: ['blur'] },
    {
      pattern: /^[a-zA-Z0-9_]{4,20}$/,
      message: '4-20 位字母/数字/下划线',
      trigger: ['blur']
    }
  ],
  adminPassword: [
    { required: true, message: '请输入密码', trigger: ['blur'] },
    { min: 6, message: '密码不少于 6 位', trigger: ['blur'] }
  ],
  adminPhone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入合法手机号',
      trigger: ['blur'],
      required: false
    }
  ]
}

async function next() {
  if (!formRef.value) {
    step.value++
    return
  }
  // 当前 step 表单分段校验：仅校验当前 step 涉及字段
  try {
    await formRef.value.validate(undefined, (rule) => {
      if (step.value === 1) return ['tenantName'].includes(rule?.key as string)
      if (step.value === 2) {
        return ['adminUsername', 'adminPassword', 'adminPhone'].includes(rule?.key as string)
      }
      return true
    })
    step.value++
  } catch {
    message.error('请补全必填项')
  }
}

async function submit() {
  submitting.value = true
  try {
    const result = await tenantApi.registerTenant(form)
    message.success(`注册成功，租户编码 ${result.tenantCode}`)
    if (result.login?.token) {
      // 已自动登录，落库 token，跳到后台首页
      localStorage.setItem('token', result.login.token)
      router.push('/dashboard')
    } else {
      router.push('/login')
    }
  } catch (e: any) {
    message.error(e?.message || '注册失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.tenant-register {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 16px;
  background: linear-gradient(135deg, #f0f5ff 0%, #fff 60%);
}
.hero {
  text-align: center;
  margin-bottom: 24px;
}
.hero h1 {
  font-size: 32px;
  margin: 0 0 8px;
  color: #1f2937;
}
.hero p {
  margin: 0;
  color: #6b7280;
}
.form-card {
  width: 100%;
  max-width: 640px;
}
</style>
