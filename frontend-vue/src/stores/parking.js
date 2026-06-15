import { defineStore } from 'pinia'
import { getSpaces, updateSpaceStatusApi, getWhiteList, getRecords, anchorUp, anchorDown } from '../utils/api'
import { WSClient } from '../utils/websocket'

const mapDbStatusToFront = (dbStatus) => {
  switch (dbStatus) {
    case 0: return 'free'
    case 1: return 'occupied'
    case 2: return 'guiding'
    default: return 'free'
  }
}

const mapSpaceFromBackend = (s) => ({
  id: s.id,
  name: s.spaceCode || s.name,
  spaceCode: s.spaceCode,
  area: s.area,
  status: s.status && typeof s.status === 'string' ? s.status : mapDbStatusToFront(s.status),
  x: s.x !== undefined ? s.x : s.xPos,
  y: s.y !== undefined ? s.y : s.yPos,
  width: s.width || 100,
  height: s.height || 60,
  color: s.color,
  speakerId: s.speakerId,
  announcing: s.announcing === true,
  announceText: s.announceText
})

export const useParkingStore = defineStore('parking', {
  state: () => ({
    spaces: [],
    wsConnected: false,
    whiteList: [],
    records: [],
    wsClient: null,
    eventLogs: []
  }),

  getters: {
    totalCount: (state) => state.spaces.length,
    occupiedCount: (state) => state.spaces.filter(s => s.status === 'occupied').length,
    freeCount: (state) => state.spaces.filter(s => s.status === 'free').length,
    guidingCount: (state) => state.spaces.filter(s => s.status === 'guiding').length,
    whiteListCount: (state) => state.whiteList.length
  },

  actions: {
    async fetchSpaces() {
      try {
        const data = await getSpaces()
        if (Array.isArray(data)) {
          this.spaces = data.map(mapSpaceFromBackend)
        } else {
          throw new Error('返回数据格式错误')
        }
      } catch (e) {
        console.error('获取车位信息失败，使用模拟数据', e)
        this.spaces = [
          { id: 1, name: 'P001', spaceCode: 'P001', status: 'free', x: 50, y: 50, width: 100, height: 60 },
          { id: 2, name: 'P002', spaceCode: 'P002', status: 'occupied', x: 180, y: 50, width: 100, height: 60 },
          { id: 3, name: 'P003', spaceCode: 'P003', status: 'free', x: 310, y: 50, width: 100, height: 60 },
          { id: 4, name: 'P004', spaceCode: 'P004', status: 'guiding', x: 440, y: 50, width: 100, height: 60 },
          { id: 5, name: 'P005', spaceCode: 'P005', status: 'free', x: 50, y: 150, width: 100, height: 60 },
          { id: 6, name: 'P006', spaceCode: 'P006', status: 'occupied', x: 180, y: 150, width: 100, height: 60 },
          { id: 7, name: 'P007', spaceCode: 'P007', status: 'free', x: 310, y: 150, width: 100, height: 60 },
          { id: 8, name: 'P008', spaceCode: 'P008', status: 'free', x: 440, y: 150, width: 100, height: 60 }
        ]
      }
    },

    connectWS() {
      if (this.wsClient) return
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsUrl = `${protocol}//${window.location.host}/ws/parking`
      this.wsClient = new WSClient(wsUrl)
      this.wsClient.onOpen = () => {
        this.wsConnected = true
        console.log('[WS] 连接已建立')
      }
      this.wsClient.onClose = () => {
        this.wsConnected = false
        console.log('[WS] 连接已关闭')
      }
      this.wsClient.onMessage = (msg) => {
        if (msg.type === 'space_update') {
          this.updateSpaceStatus(msg.spaceId, msg.status, msg.color, msg)
        } else if (msg.type === 'event_log') {
          this.addEventLog(msg.data)
        }
      }
      this.wsClient.connect()
    },

    disconnectWS() {
      if (this.wsClient) {
        this.wsClient.disconnect()
        this.wsClient = null
      }
      this.wsConnected = false
    },

    updateSpaceStatus(spaceId, status, color, rawMsg) {
      const space = this.spaces.find(s => s.id === spaceId)
      if (space) {
        if (status) space.status = status
        if (color) space.color = color
        if (rawMsg) {
          if (rawMsg.x !== undefined) space.x = rawMsg.x
          if (rawMsg.y !== undefined) space.y = rawMsg.y
          if (rawMsg.width !== undefined) space.width = rawMsg.width
          if (rawMsg.height !== undefined) space.height = rawMsg.height
          if (rawMsg.name) space.name = rawMsg.name
          if (rawMsg.speakerId !== undefined) space.speakerId = rawMsg.speakerId
          if (rawMsg.announcing !== undefined) space.announcing = rawMsg.announcing
          if (rawMsg.announceText !== undefined) space.announceText = rawMsg.announceText
        }
      }
    },

    async controlAnchor(spaceCode, action) {
      try {
        if (action === 'up') {
          await anchorUp(spaceCode)
        } else {
          await anchorDown(spaceCode)
        }
        return true
      } catch (e) {
        console.error('地锚控制失败', e)
        return false
      }
    },

    async updateSpaceStatusRemote(spaceId, status) {
      try {
        await updateSpaceStatusApi(spaceId, status)
        this.updateSpaceStatus(spaceId, mapDbStatusToFront(status))
      } catch (e) {
        console.error('更新车位状态失败', e)
      }
    },

    addEventLog(log) {
      this.eventLogs.unshift({
        id: Date.now() + Math.random(),
        time: new Date(log.time || Date.now()).toLocaleTimeString('zh-CN', { hour12: false }),
        ...log
      })
      if (this.eventLogs.length > 10) {
        this.eventLogs = this.eventLogs.slice(0, 10)
      }
    },

    async fetchWhiteList() {
      try {
        const data = await getWhiteList()
        this.whiteList = Array.isArray(data) ? data : []
      } catch (e) {
        console.error('获取白名单失败', e)
        this.whiteList = []
      }
    },

    setWhiteList(list) {
      this.whiteList = list
    },

    addWhiteListItem(item) {
      this.whiteList.push(item)
    },

    removeWhiteListItem(id) {
      this.whiteList = this.whiteList.filter(item => item.id !== id)
    },

    async fetchRecords() {
      try {
        const data = await getRecords()
        this.records = Array.isArray(data) ? data : []
      } catch (e) {
        console.error('获取记录失败', e)
        this.records = []
      }
    },

    setRecords(list) {
      this.records = list
    },

    addRecord(record) {
      this.records.unshift(record)
    }
  }
})
