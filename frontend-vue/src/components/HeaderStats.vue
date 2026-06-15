<template>
  <div class="header-stats">
    <div class="stat-card stat-total">
      <div class="stat-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="3" width="7" height="7" rx="1"/>
          <rect x="14" y="3" width="7" height="7" rx="1"/>
          <rect x="3" y="14" width="7" height="7" rx="1"/>
          <rect x="14" y="14" width="7" height="7" rx="1"/>
        </svg>
      </div>
      <div class="stat-content">
        <div class="stat-label">总车位</div>
        <div class="stat-value">{{ store.totalCount }}</div>
      </div>
    </div>

    <div class="stat-card stat-free">
      <div class="stat-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <path d="M8 12l3 3 5-6"/>
        </svg>
      </div>
      <div class="stat-content">
        <div class="stat-label">空闲</div>
        <div class="stat-value">{{ store.freeCount }}</div>
      </div>
    </div>

    <div class="stat-card stat-occupied">
      <div class="stat-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="2" y="8" width="20" height="12" rx="2"/>
          <path d="M6 8V5a2 2 0 012-2h8a2 2 0 012 2v3"/>
          <circle cx="8" cy="14" r="1.5"/>
          <circle cx="16" cy="14" r="1.5"/>
        </svg>
      </div>
      <div class="stat-content">
        <div class="stat-label">占用</div>
        <div class="stat-value">{{ store.occupiedCount }}</div>
      </div>
    </div>

    <div class="stat-card stat-whitelist">
      <div class="stat-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 12l2 2 4-4"/>
          <path d="M21 12c0 4.97-4.03 9-9 9s-9-4.03-9-9 4.03-9 9-9c2.39 0 4.68.94 6.36 2.64"/>
        </svg>
      </div>
      <div class="stat-content">
        <div class="stat-label">白名单</div>
        <div class="stat-value">{{ store.whiteListCount }}</div>
      </div>
    </div>

    <div class="ws-status" :class="{ connected: store.wsConnected }">
      <span class="ws-dot"></span>
      <span class="ws-text">{{ store.wsConnected ? 'WS 已连接' : 'WS 未连接' }}</span>
    </div>
  </div>
</template>

<script setup>
import { useParkingStore } from '../stores/parking'

const store = useParkingStore()
</script>

<style scoped>
.header-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 24px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.95), rgba(30, 41, 59, 0.95));
  border-bottom: 1px solid rgba(59, 130, 246, 0.2);
  backdrop-filter: blur(10px);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  background: rgba(30, 41, 59, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(59, 130, 246, 0.15);
  min-width: 180px;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  border-color: rgba(59, 130, 246, 0.4);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 24px;
}

.stat-icon svg {
  width: 28px;
  height: 28px;
}

.stat-total .stat-icon {
  background: rgba(139, 92, 246, 0.2);
  color: #a78bfa;
}

.stat-free .stat-icon {
  background: rgba(52, 211, 153, 0.2);
  color: #34d399;
}

.stat-occupied .stat-icon {
  background: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
}

.stat-whitelist .stat-icon {
  background: rgba(251, 191, 36, 0.2);
  color: #fbbf24;
}

.stat-label {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #f1f5f9;
  line-height: 1;
}

.ws-status {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 20px;
  color: #f87171;
  font-size: 13px;
}

.ws-status.connected {
  background: rgba(52, 211, 153, 0.1);
  border-color: rgba(52, 211, 153, 0.3);
  color: #34d399;
}

.ws-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
