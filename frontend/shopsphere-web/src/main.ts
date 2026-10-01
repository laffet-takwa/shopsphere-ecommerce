import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Root from './Root.vue'
import router from './router.ts'
import './style.css'

createApp(Root).use(createPinia()).use(router).mount('#app')