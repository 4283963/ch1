export class WSClient {
  constructor(url) {
    this.url = url
    this.ws = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 10
    this.reconnectDelay = 3000
    this.manualClose = false
    this.onOpen = null
    this.onClose = null
    this.onMessage = null
    this.onError = null
  }

  connect() {
    this.manualClose = false
    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const host = window.location.host
      const wsUrl = `${protocol}//${host}${this.url}`
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        this.reconnectAttempts = 0
        if (this.onOpen) this.onOpen()
      }

      this.ws.onclose = () => {
        if (this.onClose) this.onClose()
        if (!this.manualClose) {
          this.reconnect()
        }
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (this.onMessage) this.onMessage(data)
        } catch (e) {
          if (this.onMessage) this.onMessage(event.data)
        }
      }

      this.ws.onerror = (error) => {
        if (this.onError) this.onError(error)
      }
    } catch (e) {
      console.error('WebSocket连接错误:', e)
    }
  }

  reconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('达到最大重连次数')
      return
    }
    this.reconnectAttempts++
    setTimeout(() => {
      if (!this.manualClose) {
        this.connect()
      }
    }, this.reconnectDelay)
  }

  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(typeof data === 'string' ? data : JSON.stringify(data))
    }
  }

  disconnect() {
    this.manualClose = true
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }
}
