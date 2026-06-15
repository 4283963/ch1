import { createRouter, createWebHistory } from 'vue-router'
import ParkingMap from '../views/ParkingMap.vue'
import AdminPanel from '../views/AdminPanel.vue'

const routes = [
  {
    path: '/',
    name: 'ParkingMap',
    component: ParkingMap
  },
  {
    path: '/admin',
    name: 'AdminPanel',
    component: AdminPanel
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
