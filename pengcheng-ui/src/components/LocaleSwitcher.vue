<template>
  <n-dropdown
    :options="localeOptions"
    trigger="click"
    @select="handleSelect"
  >
    <n-button quaternary circle :title="t('locale.' + currentLocaleKey)">
      <template #icon>
        <n-icon>
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="2" y1="12" x2="22" y2="12"/>
            <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
          </svg>
        </n-icon>
      </template>
    </n-button>
  </n-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale, type SupportedLocale } from '@/i18n'
import { setMyLocale } from '@/api/i18n'

const { t, locale } = useI18n()

/** 当前 locale 对应的短 key（zh / en），用于 t('locale.xxx') */
const currentLocaleKey = computed(() => (locale.value === 'zh-CN' ? 'zh' : 'en'))

const localeOptions = computed(() => [
  {
    label: t('locale.zh'),
    key: 'zh-CN',
  },
  {
    label: t('locale.en'),
    key: 'en-US',
  },
])

async function handleSelect(key: string) {
  const target = key as SupportedLocale
  await setLocale(target)
  // 持久化到服务端（已登录用户），失败时不影响本地切换
  setMyLocale(target).catch(() => {
    /* ignore — user may not be logged in */
  })
}
</script>
