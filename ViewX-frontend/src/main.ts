import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import './styles/tailwind.css'
import './styles/theme.scss'
import i18n from './i18n'
import router from './router'

const app = createApp(App)
const pinia = createPinia()

// 配置Element Plus
app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.use(i18n)

// 挂载应用
app.mount('#app')