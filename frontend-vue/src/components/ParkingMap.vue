<template>
  <div class="parking-map-container" ref="containerRef">
    <canvas ref="canvasRef" class="parking-canvas"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useParkingStore } from '../stores/parking'

const store = useParkingStore()
const containerRef = ref(null)
const canvasRef = ref(null)
let ctx = null
let animationId = null
let pulsePhase = 0

const spaces = computed(() => store.spaces)

const getStatusColor = (status) => {
  switch (status) {
    case 'occupied':
      return { fill: '#2563eb', stroke: '#60a5fa', glow: 'rgba(37, 99, 235, 0.6)' }
    case 'guiding':
      return { fill: '#059669', stroke: '#34d399', glow: 'rgba(5, 150, 105, 0.8)' }
    case 'free':
    default:
      return { fill: '#4b5563', stroke: '#6b7280', glow: 'rgba(75, 85, 99, 0.3)' }
  }
}

const resizeCanvas = () => {
  if (!containerRef.value || !canvasRef.value) return
  const rect = containerRef.value.getBoundingClientRect()
  const dpr = window.devicePixelRatio || 1
  canvasRef.value.width = rect.width * dpr
  canvasRef.value.height = rect.height * dpr
  canvasRef.value.style.width = rect.width + 'px'
  canvasRef.value.style.height = rect.height + 'px'
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

const drawBackground = (w, h) => {
  ctx.fillStyle = '#0f172a'
  ctx.fillRect(0, 0, w, h)

  ctx.strokeStyle = 'rgba(59, 130, 246, 0.08)'
  ctx.lineWidth = 1
  const gridSize = 40
  for (let x = 0; x <= w; x += gridSize) {
    ctx.beginPath()
    ctx.moveTo(x, 0)
    ctx.lineTo(x, h)
    ctx.stroke()
  }
  for (let y = 0; y <= h; y += gridSize) {
    ctx.beginPath()
    ctx.moveTo(0, y)
    ctx.lineTo(w, y)
    ctx.stroke()
  }
}

const drawSpace = (space, scale, offsetX, offsetY) => {
  const colors = getStatusColor(space.status)
  const x = space.x * scale + offsetX
  const y = space.y * scale + offsetY
  const w = space.width * scale
  const h = space.height * scale

  if (space.status === 'guiding') {
    const pulse = 0.5 + 0.5 * Math.sin(pulsePhase)
    const glowRadius = 15 + pulse * 15
    const gradient = ctx.createRadialGradient(x + w / 2, y + h / 2, 0, x + w / 2, y + h / 2, Math.max(w, h) + glowRadius)
    gradient.addColorStop(0, `rgba(52, 211, 153, ${0.4 + pulse * 0.3})`)
    gradient.addColorStop(0.5, `rgba(5, 150, 105, ${0.2 + pulse * 0.2})`)
    gradient.addColorStop(1, 'rgba(5, 150, 105, 0)')
    ctx.fillStyle = gradient
    ctx.fillRect(x - glowRadius, y - glowRadius, w + glowRadius * 2, h + glowRadius * 2)
  }

  ctx.fillStyle = colors.fill
  ctx.strokeStyle = colors.stroke
  ctx.lineWidth = 2

  if (space.status === 'occupied') {
    ctx.shadowColor = colors.glow
    ctx.shadowBlur = 10
  } else if (space.status === 'guiding') {
    ctx.shadowColor = colors.glow
    ctx.shadowBlur = 20
  }

  roundRect(ctx, x, y, w, h, 6)
  ctx.fill()
  ctx.stroke()
  ctx.shadowBlur = 0

  ctx.fillStyle = '#ffffff'
  ctx.font = `bold ${Math.max(12, Math.min(w, h) * 0.28)}px sans-serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(space.name, x + w / 2, y + h / 2)

  const statusText = space.status === 'occupied' ? '占用' : space.status === 'guiding' ? '引导中' : '空闲'
  ctx.font = `${Math.max(10, Math.min(w, h) * 0.18)}px sans-serif`
  ctx.fillStyle = space.status === 'guiding' ? '#a7f3d0' : space.status === 'occupied' ? '#93c5fd' : '#9ca3af'
  ctx.fillText(statusText, x + w / 2, y + h * 0.75)

  if (space.announcing) {
    const announcePulse = 0.5 + 0.5 * Math.sin(pulsePhase * 2)
    const iconSize = Math.max(14, Math.min(w, h) * 0.22)
    const iconX = x + w - iconSize * 0.7
    const iconY = y + iconSize * 0.7

    const glow = ctx.createRadialGradient(iconX, iconY, 0, iconX, iconY, iconSize * 1.5)
    glow.addColorStop(0, `rgba(239, 68, 68, ${0.5 + announcePulse * 0.4})`)
    glow.addColorStop(1, 'rgba(239, 68, 68, 0)')
    ctx.fillStyle = glow
    ctx.beginPath()
    ctx.arc(iconX, iconY, iconSize * 1.5, 0, Math.PI * 2)
    ctx.fill()

    ctx.fillStyle = `rgba(239, 68, 68, ${0.7 + announcePulse * 0.3})`
    ctx.beginPath()
    ctx.moveTo(iconX - iconSize * 0.4, iconY - iconSize * 0.2)
    ctx.lineTo(iconX + iconSize * 0.15, iconY - iconSize * 0.45)
    ctx.lineTo(iconX + iconSize * 0.15, iconY + iconSize * 0.45)
    ctx.lineTo(iconX - iconSize * 0.4, iconY + iconSize * 0.2)
    ctx.closePath()
    ctx.fill()

    ctx.fillStyle = `rgba(239, 68, 68, ${0.85 + announcePulse * 0.15})`
    ctx.fillRect(iconX - iconSize * 0.5, iconY - iconSize * 0.18, iconSize * 0.18, iconSize * 0.36)

    for (let i = 0; i < 2; i++) {
      const waveAmp = 0.25 + announcePulse * 0.15
      const waveOffset = iconSize * (0.35 + i * 0.2)
      ctx.strokeStyle = `rgba(255, 255, 255, ${0.7 + announcePulse * 0.3})`
      ctx.lineWidth = Math.max(1.5, iconSize * 0.08)
      ctx.beginPath()
      ctx.arc(iconX + iconSize * 0.15, iconY, waveOffset, -Math.PI * waveAmp, Math.PI * waveAmp)
      ctx.stroke()
    }

    ctx.font = `bold ${Math.max(9, Math.min(w, h) * 0.14)}px sans-serif`
    ctx.fillStyle = '#fecaca'
    ctx.textAlign = 'center'
    ctx.fillText('语音播报中', x + w / 2, y + h * 0.92)
  }
}

const roundRect = (ctx, x, y, w, h, r) => {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h - r)
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
}

const calculateScale = () => {
  if (!containerRef.value || spaces.value.length === 0) return { scale: 1, offsetX: 0, offsetY: 0 }
  const rect = containerRef.value.getBoundingClientRect()
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
  spaces.value.forEach(s => {
    minX = Math.min(minX, s.x)
    minY = Math.min(minY, s.y)
    maxX = Math.max(maxX, s.x + s.width)
    maxY = Math.max(maxY, s.y + s.height)
  })
  const padding = 60
  const contentW = maxX - minX + padding * 2
  const contentH = maxY - minY + padding * 2
  const scaleX = rect.width / contentW
  const scaleY = rect.height / contentH
  const scale = Math.min(scaleX, scaleY) * 0.95
  const offsetX = (rect.width - (maxX - minX) * scale) / 2 - minX * scale
  const offsetY = (rect.height - (maxY - minY) * scale) / 2 - minY * scale
  return { scale, offsetX, offsetY }
}

const draw = () => {
  if (!ctx || !canvasRef.value || !containerRef.value) return
  const rect = containerRef.value.getBoundingClientRect()
  drawBackground(rect.width, rect.height)

  const { scale, offsetX, offsetY } = calculateScale()
  spaces.value.forEach(space => drawSpace(space, scale, offsetX, offsetY))

  pulsePhase += 0.05
  animationId = requestAnimationFrame(draw)
}

watch(() => store.spaces, () => {
  draw()
}, { deep: true })

onMounted(() => {
  ctx = canvasRef.value.getContext('2d')
  resizeCanvas()
  window.addEventListener('resize', resizeCanvas)
  store.fetchSpaces()
  draw()
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCanvas)
  if (animationId) cancelAnimationFrame(animationId)
})
</script>

<style scoped>
.parking-map-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #0f172a;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.parking-canvas {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
