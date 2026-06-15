<template>
  <div class="parking-view">
    <HeaderStats />
    <div class="main-content">
      <div class="map-section">
        <div class="section-title">
          <span class="title-icon"></span>
          停车场平面图
        </div>
        <div class="map-wrapper">
          <ParkingMapCanvas />
        </div>
      </div>
      <div class="log-section">
        <div class="section-title">
          <span class="title-icon log-icon"></span>
          实时事件日志
          <span class="log-count">{{ store.eventLogs.length }}/10</span>
        </div>
        <div class="log-list">
          <div v-for="log in store.eventLogs" :key="log.id" class="log-item" :class="log.eventType || log.type">
            <div class="log-header">
              <span class="log-badge">{{ getBadgeText(log.eventType || log.type) }}</span>
              <span class="log-time">{{ log.time }}</span>
            </div>
            <div class="log-content">
              <div class="log-plate">{{ log.plateNumber || log.plate || '未知车牌' }}</div>
              <div class="log-desc">{{ log.description || getDefaultDesc(log.eventType || log.type) }}</div>
            </div>
          </div>
          <div v-if="store.eventLogs.length === 0" class="empty-log">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
            </div>
            <div>暂无事件记录</div>
          </div>
        </div>
        <div class="log-legend">
          <div class="legend-item"><span class="legend-dot in"></span>进场</div>
          <div class="legend-item"><span class="legend-dot out"></span>出场</div>
          <div class="legend-item"><span class="legend-dot alert"></span>告警</div>
        </div>
      </div>
    </div>
    <div class="footer-bar">
      <div class="nav-links">
        <router-link to="/" class="nav-link active">大屏监控</router-link>
        <router-link to="/admin" class="nav-link">管理后台</router-link>
      </div>
      <div class="footer-info">
        <span class="system-title">智能停车管理系统 v1.0</span>
        <span class="current-time">{{ currentTime }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useParkingStore } from '../stores/parking'
import HeaderStats from '../components/HeaderStats.vue'
import ParkingMapCanvas from '../components/ParkingMap.vue'

const store = useParkingStore()
const currentTime = ref('')
let timer = null

const updateTime = () => {
  currentTime.value = new Date().toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

const getBadgeText = (type) => {
  const map = { in: '进场', out: '出场', alert: '告警', info: '信息' }
  return map[type] || '事件'
}

const getDefaultDesc = (type) => {
  const map = { in: '车辆进入停车场', out: '车辆离开停车场', alert: '异常事件警告', info: '系统通知' }
  return map[type] || '事件记录'
}

onMounted(() => {
  store.fetchSpaces()
  store.connectWS()
  updateTime()
  timer = setInterval(updateTime, 1000)
  if (store.eventLogs.length === 0) {
    store.addEventLog({ type: 'in', plate: '京A12345', description: '车辆进入停车场，白名单验证通过' })
    store.addEventLog({ type: 'out', plate: '沪B67890', description: '车辆离场，费用结算完成 ¥15.00' })
    store.addEventLog({ type: 'in', plate: '粤C24680', description: '引导至车位 A04' })
    store.addEventLog({ type: 'alert', plate: '未知', description: '检测到非白名单车辆，系统已记录' })
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.parking-view {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #0a1628 0%, #0f172a 100%);
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  gap: 20px;
  padding: 20px;
  min-height: 0;
}

.map-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #e2e8f0;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.15);
}

.title-icon {
  width: 4px;
  height: 18px;
  background: linear-gradient(180deg, #3b82f6, #8b5cf6);
  border-radius: 2px;
}

.log-icon {
  background: linear-gradient(180deg, #34d399, #059669);
}

.map-wrapper {
  flex: 1;
  min-height: 0;
}

.log-section {
  width: 340px;
  display: flex;
  flex-direction: column;
  background: rgba(15, 23, 42, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(59, 130, 246, 0.15);
  padding: 18px;
}

.log-count {
  margin-left: auto;
  font-size: 12px;
  color: #64748b;
  font-weight: normal;
}

.log-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}

.log-list::-webkit-scrollbar {
  width: 4px;
}

.log-list::-webkit-scrollbar-track {
  background: rgba(30, 41, 59, 0.5);
  border-radius: 2px;
}

.log-list::-webkit-scrollbar-thumb {
  background: rgba(59, 130, 246, 0.3);
  border-radius: 2px;
}

.log-item {
  padding: 12px;
  background: rgba(30, 41, 59, 0.5);
  border-radius: 8px;
  border-left: 3px solid #64748b;
  transition: all 0.2s;
}

.log-item:hover {
  background: rgba(30, 41, 59, 0.8);
  transform: translateX(2px);
}

.log-item.in {
  border-left-color: #34d399;
}

.log-item.out {
  border-left-color: #60a5fa;
}

.log-item.alert {
  border-left-color: #f59e0b;
}

.log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.log-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(100, 116, 139, 0.2);
  color: #94a3b8;
}

.log-item.in .log-badge {
  background: rgba(52, 211, 153, 0.15);
  color: #34d399;
}

.log-item.out .log-badge {
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
}

.log-item.alert .log-badge {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}

.log-time {
  font-size: 11px;
  color: #64748b;
}

.log-plate {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
  letter-spacing: 1px;
  margin-bottom: 4px;
}

.log-desc {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.4;
}

.empty-log {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #475569;
  gap: 12px;
}

.empty-icon svg {
  width: 48px;
  height: 48px;
  opacity: 0.5;
}

.log-legend {
  display: flex;
  gap: 16px;
  padding-top: 14px;
  margin-top: 14px;
  border-top: 1px solid rgba(59, 130, 246, 0.1);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-dot.in {
  background: #34d399;
}

.legend-dot.out {
  background: #60a5fa;
}

.legend-dot.alert {
  background: #f59e0b;
}

.footer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: rgba(15, 23, 42, 0.95);
  border-top: 1px solid rgba(59, 130, 246, 0.15);
}

.nav-links {
  display: flex;
  gap: 8px;
}

.nav-link {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 13px;
  color: #94a3b8;
  text-decoration: none;
  transition: all 0.2s;
}

.nav-link:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #60a5fa;
}

.nav-link.active {
  background: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.footer-info {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 12px;
  color: #64748b;
}

.system-title {
  color: #475569;
}

.current-time {
  font-family: 'Monaco', 'Menlo', monospace;
  color: #94a3b8;
  letter-spacing: 0.5px;
}
</style>
