<template>
  <div 
    class="absolute bottom-0 left-0 right-0 z-[60] transition-all duration-200"
    :class="isSeeking ? 'pb-4' : 'pb-1'"
    style="bottom: calc(env(safe-area-inset-bottom, 0px) + 47px);"
    @touchstart.stop
    @touchmove.stop
    @touchend.stop
  >
    <!-- 时间显示 (仅拖动时显示，位置在进度条上方) -->
    <div 
      v-if="isSeeking"
      class="absolute bottom-8 left-0 right-0 flex justify-center items-center pointer-events-none"
    >
      <div class="text-xl font-bold text-white drop-shadow-md bg-black/40 px-4 py-1.5 rounded-lg backdrop-blur-sm border border-white/10">
        <span class="text-white">{{ formatTime(currentTime) }}</span>
        <span class="text-white/60 mx-1">/</span>
        <span class="text-white/60 text-lg">{{ formatTime(duration) }}</span>
      </div>
    </div>

    <!-- 触控热区容器 -->
    <div 
      ref="progressBarRef"
      class="relative h-6 cursor-pointer group touch-none mx-3 flex items-center"
      @touchstart="startSeeking"
      @touchmove="handleTouchMove"
      @touchend="endSeeking"
      @touchcancel="endSeeking"
      @click="handleProgressClick"
    >
      <!-- 进度条背景轨 -->
      <div 
        class="relative w-full rounded-full transition-all duration-150 overflow-hidden backdrop-blur-sm"
        :class="isSeeking ? 'h-2 bg-white/30' : 'h-[2px] bg-white/20'"
      >
        <!-- 已播放进度 -->
        <div 
          class="absolute left-0 top-0 h-full transition-all duration-75 ease-linear rounded-full"
          :class="isSeeking ? 'bg-white' : 'bg-white/80'"
          :style="{ width: `${Math.max(progress || 0, 0)}%` }"
        ></div>
      </div>

      <!-- 进度点 (仅拖动时显示) -->
      <div 
        class="absolute top-1/2 -translate-y-1/2 w-8 h-8 -ml-4 pointer-events-none flex items-center justify-center transition-transform duration-200"
        :class="isSeeking ? 'scale-100' : 'scale-0 opacity-0'"
        :style="{ left: `${progress}%` }"
      >
        <div class="w-4 h-4 bg-white rounded-full shadow-md"></div>
      </div>
    </div>
  </div>
  
  <!-- 倍速弹窗 -->
  <transition name="fade-scale">
    <div 
      v-if="showSpeedPopup" 
      class="absolute inset-0 flex items-center justify-center z-50 bg-black/50 backdrop-blur-sm"
      @touchend="closeSpeedPopup"
    >
      <div class="bg-black/90 backdrop-blur-md rounded-2xl p-6 shadow-2xl border border-white/10 min-w-[200px]">
        <div class="text-white text-center mb-4 font-semibold">播放速度</div>
        <div class="grid grid-cols-3 gap-3">
          <button 
            v-for="rate in [0.5, 0.75, 1, 1.25, 1.5, 2]" 
            :key="rate"
            @click.stop="setPlaybackRate(rate)"
            class="px-4 py-3 text-sm rounded-xl transition-all active:scale-95"
            :class="playerStore.playbackRate === rate 
              ? 'bg-red-500 text-white font-bold shadow-lg' 
              : 'bg-white/10 text-white hover:bg-white/20'"
          >
            {{ rate }}x
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useVideoPlayerStore } from '@/stores/videoPlayer'

const props = defineProps<{
  videoElement: HTMLVideoElement | null
  isActive: boolean
}>()

const emit = defineEmits<{
  (e: 'play'): void
  (e: 'pause'): void
}>()

const playerStore = useVideoPlayerStore()

const currentTime = ref(0)
const duration = ref(0)
const isSeeking = ref(false)
const progressBarRef = ref<HTMLElement | null>(null)
const showSpeedPopup = ref(false)

// 长按检测
let longPressTimer: any = null

// 计算进度百分比
const progress = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

// 格式化时间
const formatTime = (seconds: number) => {
  if (!seconds || !isFinite(seconds)) return '0:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 设置播放速度
const setPlaybackRate = (rate: number) => {
  playerStore.setPlaybackRate(rate)
  if (props.videoElement) {
    props.videoElement.playbackRate = rate
  }
  closeSpeedPopup()
}

// 关闭倍速弹窗
const closeSpeedPopup = () => {
  showSpeedPopup.value = false
}

// 开始拖动进度条
const startSeeking = (e: TouchEvent) => {
  if (!props.videoElement || !progressBarRef.value) return
  
  // 阻止默认行为，防止页面滚动
  e.preventDefault()
  e.stopPropagation()
  
  isSeeking.value = true
  // 暂停视频以获得更平滑的拖动体验（可选，取决于需求）
  // props.videoElement.pause()
  
  updateProgress(e)
}

const handleTouchMove = (e: TouchEvent) => {
  if (!isSeeking.value) return
  e.preventDefault()
  e.stopPropagation()
  updateProgress(e)
}

const endSeeking = (e: TouchEvent) => {
  e.preventDefault()
  e.stopPropagation()
  
  isSeeking.value = false
  
  // 可以在这里恢复播放
  // if (playerStore.isPlaying) props.videoElement.play()
  
  // 延迟隐藏，让用户看到最终位置
  setTimeout(() => {
    isSeeking.value = false
  }, 1000)
}

// 点击进度条跳转 (鼠标备用)
const handleProgressClick = (e: MouseEvent) => {
  if (!props.videoElement || !progressBarRef.value) return
  
  const rect = progressBarRef.value.getBoundingClientRect()
  const x = Math.max(0, Math.min(e.clientX - rect.left, rect.width))
  const percentage = x / rect.width
  
  props.videoElement.currentTime = percentage * duration.value
}

// 更新进度
const updateProgress = (e: TouchEvent) => {
  if (!props.videoElement || !progressBarRef.value) return
  
  const rect = progressBarRef.value.getBoundingClientRect()
  const clientX = e.touches[0].clientX
  const x = Math.max(0, Math.min(clientX - rect.left, rect.width))
  const percentage = x / rect.width
  
  props.videoElement.currentTime = percentage * duration.value
  // 立即更新显示
  currentTime.value = props.videoElement.currentTime
}

// 监听视频时间更新
const handleTimeUpdate = () => {
  if (!props.videoElement || isSeeking.value) return
  currentTime.value = props.videoElement.currentTime
}

// 监听视频元数据加载
const handleLoadedMetadata = () => {
  if (!props.videoElement) return
  duration.value = props.videoElement.duration
  // 应用播放速度
  props.videoElement.playbackRate = playerStore.playbackRate
}

// 监听 videoElement 变化
watch(() => props.videoElement, (newVideo, oldVideo) => {
  // 移除旧视频的事件监听
  if (oldVideo) {
    oldVideo.removeEventListener('timeupdate', handleTimeUpdate)
    oldVideo.removeEventListener('loadedmetadata', handleLoadedMetadata)
  }
  
  // 添加新视频的事件监听
  if (newVideo) {
    newVideo.addEventListener('timeupdate', handleTimeUpdate)
    newVideo.addEventListener('loadedmetadata', handleLoadedMetadata)
    
    // 立即应用设置
    if (newVideo.readyState >= 1) {
      duration.value = newVideo.duration
      newVideo.playbackRate = playerStore.playbackRate
    }
  }
}, { immediate: true })

// 导出长按触发倍速的方法，供父组件调用
defineExpose({
  showSpeedPopup: () => {
    showSpeedPopup.value = true
  }
})

onMounted(() => {
  // 恢复保存的设置
  playerStore.restoreSettings()
})

onUnmounted(() => {
  // 清理视频事件监听
  if (props.videoElement) {
    props.videoElement.removeEventListener('timeupdate', handleTimeUpdate)
    props.videoElement.removeEventListener('loadedmetadata', handleLoadedMetadata)
  }
  
  if (longPressTimer) {
    clearTimeout(longPressTimer)
  }
})
</script>

<style scoped>
.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: all 0.2s ease;
}

.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

/* 确保进度条在移动端可见 */
.pb-safe {
  padding-bottom: env(safe-area-inset-bottom, 16px);
}
</style>
