import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import OrdersView from './views/OrdersView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: App },
    { path: '/account/orders', component: OrdersView },
  ],
})