import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import { initTheme } from './composables/useTheme'
import { initLocale } from './composables/useLocale'
import './style.css'

initTheme()
initLocale()

const app = createApp(App)
app.use(createPinia())
app.use(i18n)
app.use(router)
app.mount('#app')
