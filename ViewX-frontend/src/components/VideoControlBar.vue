<template>
  <div 
    class="absolute bottom-0 left-0 right-0 z-50 transition-all duration-300"
    :class="showControls ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4 pointer-events-none'"
    @mouseenter="onControlsMouseEnter"
    @mouseleave="onControlsMouseLeave"
  >
    <!-- 渐变背景 -->
    <div class="absolute inset-0 bg-gradient-to-t from-black/95 via-black/80 to-transparent pointer-events-none"></div>
    
    <div class="relative px-4 pb-3 pt-8">
      <!-- 进度条 (独立一行，在上方) -->
      <div class="mb-2">
        <div 
          ref="progressBarRef"
          class="group relative h-1 bg-white/20 rounded-full cursor-pointer hover:h-1.5 transition-all"
          @mousedown="startSeeking"
          @touchstart="startSeeking"
        >
          <!-- 已播放进度 -->
          <div 
            class="absolute left-0 top-0 h-full bg-red-500 rounded-full transition-all"
            :style="{ width: `${progress}%` }"
          ></div>
          
          <!-- 进度点 -->
          <div 
            class="absolute top-1/2 -translate-y-1/2 w-3 h-3 bg-red-500 rounded-full opacity-0 group-hover:opacity-100 transition-all shadow-lg"
            :style="{ left: `calc(${progress}% - 6px)` }"
          ></div>
        </div>
      </div>
      
      <!-- 控制按钮行 -->
      <div class="flex items-center justify-between gap-3">
        <!-- 左侧：播放/暂停 + 时间 -->
        <div class="flex items-center gap-3">
          <button 
            @click="togglePlay"
            class="p-1.5 hover:bg-white/10 rounded transition-all active:scale-95"
          >
            <Play v-if="!isPlaying" class="w-5 h-5 text-white" />
            <Pause v-else class="w-5 h-5 text-white" />
          </button>
          
          <!-- 时间显示 -->
          <div class="text-xs text-white/90 font-medium min-w-[100px]">
            {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
          </div>
        </div>
        
        <!-- 右侧：音量、倍速 -->
        <div class="flex items-center gap-2">
          <!-- 音量控制 -->
          <div class="relative">
            <button 
              @click="toggleMute"
              @mouseenter="onVolumeButtonEnter"
              @mouseleave="onVolumeButtonLeave"
              class="p-1.5 hover:bg-white/10 rounded transition-all active:scale-95"
            >
              <Volume2 v-if="!playerStore.isMuted && playerStore.volume > 0.5" class="w-5 h-5 text-white" />
              <Volume1 v-else-if="!playerStore.isMuted && playerStore.volume > 0" class="w-5 h-5 text-white" />
              <VolumeX v-else class="w-5 h-5 text-white" />
            </button>
            
            <!-- 音量滑块 (悬停显示) -->
            <div 
              class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 transition-all"
              :class="showVolumePopup ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'"
              @mouseenter="onVolumePopupEnter"
              @mouseleave="onVolumePopupLeave"
            >
              <div class="bg-black/95 backdrop-blur-md rounded-lg p-3 shadow-2xl border border-white/10">
                <div class="flex flex-col items-center gap-2">
                  <input 
                    type="range" 
                    min="0" 
                    max="100" 
                    :value="playerStore.volume * 100"
                    @input="handleVolumeChange"
                    class="volume-slider h-20 w-1 cursor-pointer"
                    orient="vertical"
                    style="writing-mode: bt-lr; -webkit-appearance: slider-vertical;"
                  />
                  <div class="text-xs text-white/80 font-medium">
                    {{ Math.round(playerStore.volume * 100) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 倍速控制 -->
          <div class="relative">
            <button 
              @mouseenter="onSpeedButtonEnter"
              @mouseleave="onSpeedButtonLeave"
              class="px-2.5 py-1 hover:bg-white/10 rounded transition-all text-xs text-white font-medium min-w-[42px]"
            >
              {{ playerStore.playbackRate === 1 ? '倍速' : playerStore.playbackRate + 'x' }}
            </button>
            
            <!-- 倍速选项 (悬停显示) -->
            <div 
              class="absolute bottom-full right-0 mb-2 transition-all"
              :class="showSpeedPopup ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'"
              @mouseenter="onSpeedPopupEnter"
              @mouseleave="onSpeedPopupLeave"
            >
              <div class="bg-black/95 backdrop-blur-md rounded-lg p-1.5 shadow-2xl border border-white/10 min-w-[70px]">
                <button 
                  v-for="rate in [0.5, 0.75, 1, 1.25, 1.5, 2]" 
                  :key="rate"
                  @click="setPlaybackRate(rate)"
                  class="w-full px-3 py-1.5 text-xs rounded hover:bg-white/10 transition-all text-left"
                  :class="playerStore.playbackRate === rate ? 'text-red-500 font-semibold bg-white/5' : 'text-white'"
                >
                  {{ rate }}x
                </button>
              </div>
            </div>
          </div>
          
          <!-- 全屏控制 -->
          <div>
            <button 
              @click="toggleFullscreen"
              class="p-1.5 hover:bg-white/10 rounded transition-all active:scale-95"
            >
              <Maximize v-if="!isFullscreen" class="w-5 h-5 text-white" />
              <Minimize v-else class="w-5 h-5 text-white" />
            </button>
          </div>

          <!-- 播放顺序控制 -->
          <div>
            <button 
              @click="toggleShuffle"
              class="p-1.5 hover:bg-white/10 rounded transition-all active:scale-95"
            >
              <Shuffle v-if="!isShuffle" class="w-5 h-5 text-white" />
              <Repeat1 v-else class="w-5 h-5 text-white" />
            </button>
          </div>

          <!-- 剧场模式控制 -->
          <div>
            <button 
              @click="toggleTheaterMode"
              class="p-1.5 hover:bg-white/10 rounded transition-all active:scale-95"
            >
              <MonitorPlay v-if="!isTheaterMode" class="w-5 h-5 text-white" />
              <MonitorStop v-else class="w-5 h-5 text-white" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { Play, Pause, Volume2, Volume1, VolumeX, Maximize, Minimize, Shuffle, Repeat1, MonitorPlay, MonitorStop } from 'lucide-vue-next'
import { useVideoPlayerStore } from '@/stores/videoPlayer'

const props = defineProps<{
  videoElement: HTMLVideoElement | null
  isActive: boolean
}>()

const emit = defineEmits<{
  (e: 'play'): void
  (e: 'pause'): void
  (e: 'next'): void
  (e: 'prev'): void
  (e: 'toggleFullscreen'): void
  (e: 'toggleTheaterMode'): void
  (e: 'toggleShuffle'): void
}>()

const playerStore = useVideoPlayerStore()

const showControls = ref(true)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const isSeeking = ref(false)
const progressBarRef = ref<HTMLElement | null>(null)
const isFullscreen = ref(false)
const isShuffle = ref(false)
const isTheaterMode = ref(false)

const toggleFullscreen = () => {
  if (!props.videoElement) return

  if (!document.fullscreenElement) {
    props.videoElement.requestFullscreen().then(() => {
      isFullscreen.value = true
    }).catch(err => {
      console.error(`Error attempting to enable full-screen mode: ${err.message} (${err.name})`);
    });
  } else {
    document.exitFullscreen().then(() => {
      isFullscreen.value = false
    }).catch(err => {
      console.error(`Error attempting to exit full-screen mode: ${err.message} (${err.name})`);
    });
  }
}

let hideControlsTimeout: any = null
const showVolumePopup = ref(false)
let volumePopupTimeout: any = null
const showSpeedPopup = ref(false)
let speedPopupTimeout: any = null

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

// 切换播放/暂停
const togglePlay = () => {
  if (!props.videoElement) return
  
  if (isPlaying.value) {
    props.videoElement.pause()
    emit('pause')
  } else {
    props.videoElement.play()
    emit('play')
  }
}

// 切换静音
const toggleMute = () => {
  playerStore.toggleMute()
  applyVolumeToVideo()
}

// 音量变化
const handleVolumeChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const newVolume = parseInt(target.value) / 100
  playerStore.setVolume(newVolume)
  applyVolumeToVideo()
}

// 设置播放速度
const setPlaybackRate = (rate: number) => {
  playerStore.setPlaybackRate(rate)
  applyPlaybackRateToVideo()
}

// 应用音量到视频元素
const applyVolumeToVideo = () => {
  if (!props.videoElement) return
  props.videoElement.volume = playerStore.isMuted ? 0 : playerStore.volume
  props.videoElement.muted = playerStore.isMuted
}

// 应用播放速度到视频元素
const applyPlaybackRateToVideo = () => {
  if (!props.videoElement) return
  props.videoElement.playbackRate = playerStore.playbackRate
}

// 开始拖动进度条
const startSeeking = (e: MouseEvent | TouchEvent) => {
  if (!props.videoElement || !progressBarRef.value) return
  
  isSeeking.value = true
  updateProgress(e)
  
  const handleMove = (e: MouseEvent | TouchEvent) => {
    if (isSeeking.value) {
      updateProgress(e)
    }
  }
  
  const handleEnd = () => {
    isSeeking.value = false
    document.removeEventListener('mousemove', handleMove)
    document.removeEventListener('mouseup', handleEnd)
    document.removeEventListener('touchmove', handleMove)
    document.removeEventListener('touchend', handleEnd)
  }
  
  document.addEventListener('mousemove', handleMove)
  document.addEventListener('mouseup', handleEnd)
  document.addEventListener('touchmove', handleMove)
  document.addEventListener('touchend', handleEnd)
}

// 更新进度
const updateProgress = (e: MouseEvent | TouchEvent) => {
  if (!props.videoElement || !progressBarRef.value) return
  
  const rect = progressBarRef.value.getBoundingClientRect()
  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const x = Math.max(0, Math.min(clientX - rect.left, rect.width))
  const percentage = x / rect.width
  
  props.videoElement.currentTime = percentage * duration.value
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
  applyVolumeToVideo()
  applyPlaybackRateToVideo()
}

// 监听播放状态
const handlePlay = () => {
  isPlaying.value = true
  resetHideControlsTimer()
}

const handlePause = () => {
  isPlaying.value = false
  showControls.value = true
  clearTimeout(hideControlsTimeout)
}

// 切换播放顺序
const toggleShuffle = () => {
  isShuffle.value = !isShuffle.value
  // TODO: Implement actual shuffle logic
  console.log('Shuffle toggled:', isShuffle.value)
}

// 切换剧场模式
const toggleTheaterMode = () => {
  isTheaterMode.value = !isTheaterMode.value
  emit('toggleTheaterMode')
  console.log('Theater Mode toggled:', isTheaterMode.value)
}

// 处理点赞
const handleLike = () => {
  // TODO: Implement actual like logic
  console.log('Video liked!')
}

// 自动隐藏控制条
const resetHideControlsTimer = () => {
  showControls.value = true
  clearTimeout(hideControlsTimeout)
  
  // 只有在播放且没有弹出层显示时才自动隐藏
  if (isPlaying.value && !showVolumePopup.value && !showSpeedPopup.value) {
    hideControlsTimeout = setTimeout(() => {
      if (!showVolumePopup.value && !showSpeedPopup.value) {
        showControls.value = false
      }
    }, 3000)
  }
}

// 鼠标移动时显示控制条
const handleMouseMove = () => {
  resetHideControlsTimer()
}

// 鼠标进入控制条区域
const onControlsMouseEnter = () => {
  showControls.value = true
  clearTimeout(hideControlsTimeout)
}

// 鼠标离开控制条区域
const onControlsMouseLeave = () => {
  // 只有在不悬停弹出菜单时才隐藏
  if (isPlaying.value && !showVolumePopup.value && !showSpeedPopup.value) {
    resetHideControlsTimer()
  }
}

// 音量按钮悬停事件
const onVolumeButtonEnter = () => {
  clearTimeout(volumePopupTimeout)
  showVolumePopup.value = true
  clearTimeout(hideControlsTimeout) // 保持控制条显示
}

const onVolumeButtonLeave = () => {
  volumePopupTimeout = setTimeout(() => {
    showVolumePopup.value = false
    if (isPlaying.value) resetHideControlsTimer()
  }, 100)
}

// 音量弹出层悬停事件
const onVolumePopupEnter = () => {
  clearTimeout(volumePopupTimeout)
  showVolumePopup.value = true
  clearTimeout(hideControlsTimeout) // 保持控制条显示
}

const onVolumePopupLeave = () => {
  volumePopupTimeout = setTimeout(() => {
    showVolumePopup.value = false
    if (isPlaying.value) resetHideControlsTimer()
  }, 100)
}

// 倍速按钮悬停事件
const onSpeedButtonEnter = () => {
  clearTimeout(speedPopupTimeout)
  showSpeedPopup.value = true
  clearTimeout(hideControlsTimeout) // 保持控制条显示
}

const onSpeedButtonLeave = () => {
  speedPopupTimeout = setTimeout(() => {
    showSpeedPopup.value = false
    if (isPlaying.value) resetHideControlsTimer()
  }, 100)
}

// 倍速弹出层悬停事件
const onSpeedPopupEnter = () => {
  clearTimeout(speedPopupTimeout)
  showSpeedPopup.value = true
  clearTimeout(hideControlsTimeout) // 保持控制条显示
}

const onSpeedPopupLeave = () => {
  speedPopupTimeout = setTimeout(() => {
    showSpeedPopup.value = false
    if (isPlaying.value) resetHideControlsTimer()
  }, 100)
}

// 监听 videoElement 变化
watch(() => props.videoElement, (newVideo, oldVideo) => {
  // 移除旧视频的事件监听
  if (oldVideo) {
    oldVideo.removeEventListener('timeupdate', handleTimeUpdate)
    oldVideo.removeEventListener('loadedmetadata', handleLoadedMetadata)
    oldVideo.removeEventListener('play', handlePlay)
    oldVideo.removeEventListener('pause', handlePause)
  }
  
  // 添加新视频的事件监听
  if (newVideo) {
    newVideo.addEventListener('timeupdate', handleTimeUpdate)
    newVideo.addEventListener('loadedmetadata', handleLoadedMetadata)
    newVideo.addEventListener('play', handlePlay)
    newVideo.addEventListener('pause', handlePause)
    
    // 立即应用设置
    if (newVideo.readyState >= 1) {
      duration.value = newVideo.duration
      applyVolumeToVideo()
      applyPlaybackRateToVideo()
    }
    
    isPlaying.value = !newVideo.paused
  }
}, { immediate: true })

// 监听激活状态
watch(() => props.isActive, (active) => {
  if (active) {
    resetHideControlsTimer()
  } else {
    showControls.value = false
    clearTimeout(hideControlsTimeout)
  }
})

// Keyboard event handler
const handleKeyDown = (event: KeyboardEvent) => {
  if (!props.isActive) return;
  if (!props.videoElement) return;

  // 如果焦点在输入框或可编辑元素上，不触发快捷键
  const target = event.target as HTMLElement;
  if (target.tagName === 'INPUT' || 
      target.tagName === 'TEXTAREA' || 
      target.isContentEditable) {
    return;
  }

  const video = props.videoElement;
  const seekTime = 5; // 每次快进/快退的秒数

  switch (event.key) {
    case ' ': // Spacebar to play/pause
      event.preventDefault(); // Prevent scrolling
      togglePlay();
      break;
    case 'ArrowLeft': // Left arrow to seek backward
      video.currentTime = Math.max(0, video.currentTime - seekTime);
      break;
    case 'ArrowRight': // Right arrow to seek forward
      video.currentTime = Math.min(video.duration, video.currentTime + seekTime);
      break;
    case 'ArrowUp': // 上箭头：上一个视频
      event.preventDefault();
      emit('prev');
      break;
    case 'ArrowDown': // 下箭头：下一个视频
      event.preventDefault();
      emit('next');
      break;
    case 'f': // F 键：切换全屏
    case 'F':
      event.preventDefault();
      toggleFullscreen();
      break;
    case 'm': // 'm' key to mute/unmute
    case 'M':
      playerStore.isMuted = !playerStore.isMuted;
      applyVolumeToVideo();
      break;
    case 'z': // Z 键：点赞
    case 'Z':
      event.preventDefault();
      handleLike();
      break;
    case 'u': // U 键：清屏/剧场模式
    case 'U':
      event.preventDefault();
      toggleTheaterMode();
      break;
  }
};

// 监听全屏状态变化
const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
}

onMounted(() => {
  // 恢复保存的设置
  playerStore.restoreSettings()
  
  // 监听鼠标移动
  document.addEventListener('mousemove', handleMouseMove)
  // 监听键盘事件
  document.addEventListener('keydown', handleKeyDown);
  // 监听全屏事件
  document.addEventListener('fullscreenchange', handleFullscreenChange);
})

onUnmounted(() => {
  clearTimeout(hideControlsTimeout)
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('keydown', handleKeyDown); // 移除键盘事件监听
  document.removeEventListener('fullscreenchange', handleFullscreenChange); // 移除全屏事件监听
  
  // 清理视频事件监听
  if (props.videoElement) {
    props.videoElement.removeEventListener('timeupdate', handleTimeUpdate)
    props.videoElement.removeEventListener('loadedmetadata', handleLoadedMetadata)
    props.videoElement.removeEventListener('play', handlePlay)
    props.videoElement.removeEventListener('pause', handlePause)
  }
})
</script>

<style scoped>
/* 音量滑块 - 垂直方向 */
.volume-slider {
  -webkit-appearance: slider-vertical;
  appearance: slider-vertical;
  background: transparent;
}

.volume-slider::-webkit-slider-track {
  background: rgba(255, 255, 255, 0.2);
  width: 4px;
  border-radius: 2px;
}

.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #ef4444;
  cursor: pointer;
}

.volume-slider::-moz-range-track {
  background: rgba(255, 255, 255, 0.2);
  width: 4px;
  border-radius: 2px;
}

.volume-slider::-moz-range-thumb {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #ef4444;
  cursor: pointer;
  border: none;
}
</style>
