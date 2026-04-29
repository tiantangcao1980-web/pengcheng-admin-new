import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import naive from 'naive-ui'
import App from './App.vue'
import router from './router'
import './styles/index.scss'
import { fetchCryptoConfig } from './utils/request'
import { useSiteStore } from './stores/site'
import i18n, { initI18n } from './i18n'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.use(naive)
app.use(i18n)

// 预加载加密配置
fetchCryptoConfig()

// 预加载站点配置
const siteStore = useSiteStore()
siteStore.loadConfig()

// 异步拉取服务端 i18n 词条（失败时静默降级到本地静态词条）
initI18n()

app.mount('#app')
