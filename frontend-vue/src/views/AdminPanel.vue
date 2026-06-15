<template>
  <div class="admin-panel">
    <div class="admin-header">
      <div class="header-left">
        <div class="logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 3v4a1 1 0 001 1h4"/>
            <path d="M17 21H7a2 2 0 01-2-2V5a2 2 0 012-2h7l5 5v11a2 2 0 01-2 2z"/>
          </svg>
        </div>
        <div>
          <h1>停车管理后台</h1>
          <p>管理员控制台</p>
        </div>
      </div>
      <div class="header-right">
        <router-link to="/" class="back-btn">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/>
          </svg>
          返回大屏
        </router-link>
      </div>
    </div>

    <div class="tabs-bar">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <span class="tab-icon" v-html="tab.icon"></span>
        {{ tab.label }}
      </div>
    </div>

    <div class="tab-content">
      <div v-show="activeTab === 'whitelist'" class="tab-panel">
        <div class="panel-card">
          <div class="panel-header">
            <h2>车牌白名单管理</h2>
            <button class="btn btn-primary" @click="showAddModal = true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 4v16m8-8H4"/>
              </svg>
              新增白名单
            </button>
          </div>
          <div class="table-wrapper">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>车牌号</th>
                  <th>车主姓名</th>
                  <th>联系电话</th>
                  <th>添加时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in whiteListData" :key="item.id">
                  <td>{{ item.id }}</td>
                  <td><span class="plate-tag">{{ item.plate }}</span></td>
                  <td>{{ item.owner }}</td>
                  <td>{{ item.phone }}</td>
                  <td>{{ item.createTime }}</td>
                  <td>
                    <button class="btn btn-danger btn-sm" @click="deleteWhiteItem(item.id)">
                      删除
                    </button>
                  </td>
                </tr>
                <tr v-if="whiteListData.length === 0">
                  <td colspan="6" class="empty-cell">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div v-show="activeTab === 'spaces'" class="tab-panel">
        <div class="panel-card">
          <div class="panel-header">
            <h2>车位状态控制</h2>
            <div class="stat-inline">
              <span class="badge free">空闲 {{ freeCount }}</span>
              <span class="badge occupied">占用 {{ occupiedCount }}</span>
              <span class="badge guiding">引导 {{ guidingCount }}</span>
            </div>
          </div>
          <div class="spaces-grid">
            <div v-for="space in store.spaces" :key="space.id" class="space-control-card" :class="space.status">
              <div class="space-header">
                <span class="space-name">{{ space.name }}</span>
                <span class="space-status">{{ getStatusText(space.status) }}</span>
              </div>
              <div class="space-body">
                <div class="anchor-status">
                  <span class="label">地锚状态：</span>
                  <span class="anchor-indicator" :class="space.status === 'occupied' ? 'up' : 'down'">
                    {{ space.status === 'occupied' ? '升起' : '降下' }}
                  </span>
                </div>
              </div>
              <div class="space-actions">
                <button class="btn btn-success btn-sm" @click="controlAnchor(space.id, 'up')" :disabled="space.status === 'occupied'">
                  升起地锚
                </button>
                <button class="btn btn-warning btn-sm" @click="controlAnchor(space.id, 'down')" :disabled="space.status === 'free' || space.status === 'guiding'">
                  降下地锚
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-show="activeTab === 'recognize'" class="tab-panel">
        <div class="panel-card">
          <div class="panel-header">
            <h2>模拟车牌识别</h2>
            <span class="panel-desc">模拟红外相机触发车牌识别接口</span>
          </div>
          <div class="recognize-section">
            <div class="recognize-form">
              <div class="form-group">
                <label>车牌号</label>
                <input v-model="recognizePlate" type="text" class="form-input" placeholder="请输入车牌号，如：京A12345" maxlength="8">
              </div>
              <div class="quick-plates">
                <span class="quick-label">快速选择：</span>
                <button v-for="p in quickPlates" :key="p" class="quick-btn" @click="recognizePlate = p">{{ p }}</button>
              </div>
              <div class="form-actions">
                <button class="btn btn-primary" @click="doRecognize" :disabled="!recognizePlate || recognizing">
                  <svg v-if="recognizing" viewBox="0 0 24 24" class="spin" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 12a9 9 0 11-6.219-8.56"/>
                  </svg>
                  {{ recognizing ? '识别中...' : '触发识别' }}
                </button>
              </div>
            </div>
            <div class="recognize-result" v-if="recognizeResult">
              <div class="result-header">
                <span class="result-badge" :class="recognizeResult.passed ? 'success' : 'failed'">
                  {{ recognizeResult.passed ? '识别通过' : '识别失败' }}
                </span>
              </div>
              <div class="result-body">
                <div class="result-row"><span class="k">车牌号：</span><span class="plate-tag">{{ recognizeResult.plate }}</span></div>
                <div class="result-row"><span class="k">白名单：</span><span :class="recognizeResult.inWhiteList ? 'text-success' : 'text-danger'">{{ recognizeResult.inWhiteList ? '是' : '否' }}</span></div>
                <div class="result-row" v-if="recognizeResult.guideSpace"><span class="k">引导车位：</span><span class="text-primary">{{ recognizeResult.guideSpace }}</span></div>
                <div class="result-row"><span class="k">消息：</span><span>{{ recognizeResult.message }}</span></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-show="activeTab === 'records'" class="tab-panel">
        <div class="panel-card">
          <div class="panel-header">
            <h2>进出记录</h2>
            <button class="btn btn-outline" @click="loadRecords">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M23 4v6h-6M1 20v-6h6"/>
                <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
              </svg>
              刷新
            </button>
          </div>
          <div class="table-wrapper">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>车牌号</th>
                  <th>类型</th>
                  <th>车位</th>
                  <th>时间</th>
                  <th>费用</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in recordsData" :key="r.id">
                  <td>{{ r.id }}</td>
                  <td><span class="plate-tag">{{ r.plate }}</span></td>
                  <td><span class="type-badge" :class="r.type">{{ r.type === 'in' ? '进场' : '出场' }}</span></td>
                  <td>{{ r.space || '-' }}</td>
                  <td>{{ r.time }}</td>
                  <td>{{ r.fee || '-' }}</td>
                  <td><span class="status-dot" :class="r.statusClass"></span>{{ r.status }}</td>
                </tr>
                <tr v-if="recordsData.length === 0">
                  <td colspan="7" class="empty-cell">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showAddModal" class="modal-mask" @click.self="showAddModal = false">
      <div class="modal-box">
        <div class="modal-header">
          <h3>新增白名单</h3>
          <button class="modal-close" @click="showAddModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>车牌号 *</label>
            <input v-model="addForm.plate" type="text" class="form-input" placeholder="如：京A12345">
          </div>
          <div class="form-group">
            <label>车主姓名</label>
            <input v-model="addForm.owner" type="text" class="form-input" placeholder="请输入车主姓名">
          </div>
          <div class="form-group">
            <label>联系电话</label>
            <input v-model="addForm.phone" type="text" class="form-input" placeholder="请输入联系电话">
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="showAddModal = false">取消</button>
          <button class="btn btn-primary" @click="submitAddWhite">确认添加</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useParkingStore } from '../stores/parking'
import { recognizePlate as apiRecognize, addWhiteList, deleteWhiteList, getWhiteList, getRecords } from '../utils/api'

const store = useParkingStore()

const tabs = [
  { key: 'whitelist', label: '白名单管理', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>' },
  { key: 'spaces', label: '车位控制', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>' },
  { key: 'recognize', label: '车牌识别', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 7V4h16v3M9 20h6M12 4v16"/></svg>' },
  { key: 'records', label: '进出记录', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 17v-2a4 4 0 014-4h4"/><path d="M3 7V5a2 2 0 012-2h14a2 2 0 012 2v14a2 2 0 01-2 2H5a2 2 0 01-2-2V7z"/></svg>' }
]

const activeTab = ref('whitelist')
const showAddModal = ref(false)
const addForm = reactive({ plate: '', owner: '', phone: '' })
const recognizePlate = ref('')
const recognizing = ref(false)
const recognizeResult = ref(null)

const quickPlates = ['京A12345', '沪B67890', '粤C24680', '浙D13579', '苏F86420']

const whiteListData = ref([
  { id: 1, plate: '京A12345', owner: '张三', phone: '138****1234', createTime: '2025-01-10 09:00:00' },
  { id: 2, plate: '沪B67890', owner: '李四', phone: '139****5678', createTime: '2025-01-12 14:30:00' },
  { id: 3, plate: '粤C24680', owner: '王五', phone: '137****2468', createTime: '2025-01-15 11:20:00' }
])

const recordsData = ref([
  { id: 1, plate: '京A12345', type: 'in', space: 'A01', time: '2025-01-20 08:30:15', fee: '-', status: '正常', statusClass: 'success' },
  { id: 2, plate: '浙D99999', type: 'in', space: '-', time: '2025-01-20 08:45:22', fee: '-', status: '拒绝', statusClass: 'danger' },
  { id: 3, plate: '沪B67890', type: 'out', space: 'A02', time: '2025-01-20 09:15:40', fee: '¥15.00', status: '完成', statusClass: 'success' },
  { id: 4, plate: '粤C24680', type: 'in', space: 'A04', time: '2025-01-20 09:30:08', fee: '-', status: '正常', statusClass: 'success' },
  { id: 5, plate: '京A12345', type: 'out', space: 'A01', time: '2025-01-20 18:02:33', fee: '¥60.00', status: '完成', statusClass: 'success' }
])

const freeCount = computed(() => store.spaces.filter(s => s.status === 'free').length)
const occupiedCount = computed(() => store.spaces.filter(s => s.status === 'occupied').length)
const guidingCount = computed(() => store.spaces.filter(s => s.status === 'guiding').length)

const getStatusText = (status) => {
  const map = { free: '空闲', occupied: '占用', guiding: '引导中' }
  return map[status] || status
}

const controlAnchor = async (spaceId, action) => {
  const status = action === 'up' ? 'occupied' : 'free'
  try {
    await store.updateSpaceStatusRemote(spaceId, status)
  } catch (e) {
    console.error('操作失败', e)
  }
}

const doRecognize = async () => {
  recognizing.value = true
  recognizeResult.value = null
  try {
    const res = await apiRecognize(recognizePlate.value)
    recognizeResult.value = res
  } catch (e) {
    const inWhite = whiteListData.value.some(w => w.plate === recognizePlate.value)
    const freeSpace = store.spaces.find(s => s.status === 'free')
    recognizeResult.value = {
      plate: recognizePlate.value,
      inWhiteList: inWhite,
      passed: inWhite,
      guideSpace: inWhite && freeSpace ? freeSpace.name : null,
      message: inWhite ? `白名单验证通过，${freeSpace ? `请前往 ${freeSpace.name} 车位` : '暂无空闲车位'}` : '非白名单车辆，禁止进入'
    }
    if (inWhite && freeSpace) {
      store.updateSpaceStatus(freeSpace.id, 'guiding')
    }
    store.addEventLog({
      type: inWhite ? 'in' : 'alert',
      plate: recognizePlate.value,
      description: recognizeResult.value.message
    })
  } finally {
    recognizing.value = false
  }
}

const submitAddWhite = async () => {
  if (!addForm.plate) return
  try {
    await addWhiteList(addForm)
  } catch (e) {}
  whiteListData.value.push({
    id: Date.now(),
    plate: addForm.plate,
    owner: addForm.owner || '未知',
    phone: addForm.phone || '-',
    createTime: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  })
  store.setWhiteList(whiteListData.value)
  addForm.plate = ''
  addForm.owner = ''
  addForm.phone = ''
  showAddModal.value = false
}

const deleteWhiteItem = async (id) => {
  if (!confirm('确认删除该白名单记录？')) return
  try {
    await deleteWhiteList(id)
  } catch (e) {}
  whiteListData.value = whiteListData.value.filter(w => w.id !== id)
  store.setWhiteList(whiteListData.value)
}

const loadRecords = async () => {
  try {
    const res = await getRecords()
    if (res && res.length) recordsData.value = res
  } catch (e) {}
}

onMounted(() => {
  store.fetchSpaces()
  store.setWhiteList(whiteListData.value)
})
</script>

<style scoped>
.admin-panel {
  width: 100%;
  min-height: 100vh;
  background: #f8fafc;
  color: #1e293b;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 28px;
  background: linear-gradient(135deg, #1e3a8a, #1e40af);
  color: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo svg {
  width: 24px;
  height: 24px;
}

.header-left h1 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 2px;
}

.header-left p {
  font-size: 12px;
  opacity: 0.8;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 8px;
  color: #fff;
  text-decoration: none;
  font-size: 13px;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.25);
}

.back-btn svg {
  width: 16px;
  height: 16px;
}

.tabs-bar {
  display: flex;
  gap: 4px;
  padding: 0 28px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  margin-bottom: -1px;
}

.tab-item:hover {
  color: #2563eb;
}

.tab-item.active {
  color: #2563eb;
  border-bottom-color: #2563eb;
  font-weight: 500;
}

.tab-icon svg {
  width: 18px;
  height: 18px;
}

.tab-content {
  padding: 24px 28px;
}

.tab-panel {
  animation: fadeIn 0.25s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.panel-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-header h2 {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.panel-desc {
  font-size: 12px;
  color: #94a3b8;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  text-decoration: none;
}

.btn svg {
  width: 15px;
  height: 15px;
}

.btn-sm {
  padding: 5px 12px;
  font-size: 12px;
}

.btn-primary {
  background: #2563eb;
  color: #fff;
}

.btn-primary:hover {
  background: #1d4ed8;
}

.btn-primary:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.btn-success {
  background: #059669;
  color: #fff;
}

.btn-success:hover {
  background: #047857;
}

.btn-success:disabled {
  background: #6ee7b7;
  cursor: not-allowed;
}

.btn-warning {
  background: #d97706;
  color: #fff;
}

.btn-warning:hover {
  background: #b45309;
}

.btn-warning:disabled {
  background: #fcd34d;
  cursor: not-allowed;
}

.btn-danger {
  background: #dc2626;
  color: #fff;
}

.btn-danger:hover {
  background: #b91c1c;
}

.btn-outline {
  background: transparent;
  border: 1px solid #cbd5e1;
  color: #475569;
}

.btn-outline:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
}

.stat-inline {
  display: flex;
  gap: 10px;
}

.badge {
  padding: 5px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

.badge.free {
  background: #ecfdf5;
  color: #059669;
}

.badge.occupied {
  background: #eff6ff;
  color: #2563eb;
}

.badge.guiding {
  background: #fffbeb;
  color: #d97706;
}

.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  padding: 12px 18px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.data-table td {
  padding: 14px 18px;
  font-size: 13px;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
}

.data-table tbody tr:hover {
  background: #f8fafc;
}

.empty-cell {
  text-align: center !important;
  padding: 40px !important;
  color: #94a3b8 !important;
}

.plate-tag {
  display: inline-block;
  padding: 3px 10px;
  background: linear-gradient(135deg, #1e40af, #2563eb);
  color: #fff;
  border-radius: 4px;
  font-family: 'Menlo', monospace;
  font-size: 12px;
  letter-spacing: 1px;
  font-weight: 600;
}

.type-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.type-badge.in {
  background: #ecfdf5;
  color: #059669;
}

.type-badge.out {
  background: #eff6ff;
  color: #2563eb;
}

.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 6px;
}

.status-dot.success {
  background: #10b981;
}

.status-dot.danger {
  background: #ef4444;
}

.status-dot.warning {
  background: #f59e0b;
}

.spaces-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
  padding: 22px;
}

.space-control-card {
  padding: 18px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #fff;
  transition: all 0.2s;
}

.space-control-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.space-control-card.free {
  border-left: 4px solid #6b7280;
}

.space-control-card.occupied {
  border-left: 4px solid #2563eb;
}

.space-control-card.guiding {
  border-left: 4px solid #059669;
}

.space-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #e2e8f0;
}

.space-name {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.space-status {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 4px;
  font-weight: 500;
}

.space-control-card.free .space-status {
  background: #f3f4f6;
  color: #6b7280;
}

.space-control-card.occupied .space-status {
  background: #eff6ff;
  color: #2563eb;
}

.space-control-card.guiding .space-status {
  background: #ecfdf5;
  color: #059669;
}

.space-body {
  margin-bottom: 14px;
}

.anchor-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.anchor-status .label {
  color: #64748b;
}

.anchor-indicator {
  font-weight: 600;
}

.anchor-indicator.up {
  color: #2563eb;
}

.anchor-indicator.down {
  color: #6b7280;
}

.space-actions {
  display: flex;
  gap: 8px;
}

.space-actions .btn {
  flex: 1;
  justify-content: center;
}

.recognize-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  padding: 24px;
}

.recognize-form, .recognize-result {
  background: #f8fafc;
  border-radius: 10px;
  padding: 22px;
  border: 1px solid #e2e8f0;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
  background: #fff;
}

.form-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.quick-plates {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
}

.quick-label {
  font-size: 12px;
  color: #94a3b8;
}

.quick-btn {
  padding: 5px 10px;
  border: 1px dashed #cbd5e1;
  background: #fff;
  border-radius: 4px;
  font-size: 12px;
  color: #475569;
  font-family: 'Menlo', monospace;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

.form-actions {
  display: flex;
  gap: 10px;
}

.spin {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.result-header {
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e2e8f0;
}

.result-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.result-badge.success {
  background: #ecfdf5;
  color: #059669;
}

.result-badge.failed {
  background: #fef2f2;
  color: #dc2626;
}

.result-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  font-size: 13px;
  border-bottom: 1px dashed #e2e8f0;
}

.result-row:last-child {
  border-bottom: none;
}

.result-row .k {
  width: 90px;
  color: #64748b;
}

.text-success {
  color: #059669;
  font-weight: 500;
}

.text-danger {
  color: #dc2626;
  font-weight: 500;
}

.text-primary {
  color: #2563eb;
  font-weight: 600;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.modal-box {
  width: 440px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: modalIn 0.2s ease;
}

@keyframes modalIn {
  from { opacity: 0; transform: translateY(-20px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid #f1f5f9;
}

.modal-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  font-size: 24px;
  color: #94a3b8;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-close:hover {
  background: #f1f5f9;
  color: #475569;
}

.modal-body {
  padding: 22px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 22px;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
}
</style>
