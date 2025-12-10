<template>
  <div class="relative w-full h-full bg-black snap-start snap-always shrink-0 overflow-hidden group">
    <!-- Video Player -->
    <video 
      ref="videoRef"
      :src="video.videoUrl"
      class="w-full h-full relative z-0 transition-all duration-300"
      :class="objectFitClass"
      loop
      playsinline
      webkit-playsinline
      preload="metadata"
      @click="togglePlay"
      @loadedmetadata="onMetadataLoaded"
      @timeupdate="onTimeUpdate"
      @error="onVideoError"
      @loadstart="onLoadStart"
      @canplay="onCanPlay"
      @waiting="onWaiting"
      @stalled="onStalled"
    ></video>
    
    <!-- Loading Indicator -->
    <div v-if="isLoading" class="absolute inset-0 flex items-center justify-center bg-black/50 z-20">
      <div class="flex flex-col items-center gap-3">
        <div class="w-12 h-12 border-4 border-white/30 border-t-white rounded-full animate-spin"></div>
        <div class="text-white text-sm">加载中...</div>
      </div>
    </div>
    
    <!-- Video Background (for containment) -->
    <div v-if="objectFitClass === 'object-contain'" class="absolute inset-0 z-[-1]">
       <img :src="video.thumbnailUrl" class="w-full h-full object-cover blur-3xl opacity-50" />
    </div>

    <!-- Play Overlay (Center) -->
    <div v-if="!isPlaying" class="absolute inset-0 flex items-center justify-center pointer-events-none z-10">
      <div class="w-16 h-16 bg-white/20 rounded-full flex items-center justify-center backdrop-blur-sm animate-pulse">
        <Play class="w-8 h-8 text-white fill-white ml-1" />
      </div>
    </div>

    <!-- Shadows/Gradients -->
    <div class="absolute inset-x-0 top-0 h-40 bg-gradient-to-b from-black/60 to-transparent pointer-events-none"></div>
    <div class="absolute inset-x-0 bottom-0 h-60 bg-gradient-to-t from-black/80 via-black/40 to-transparent pointer-events-none"></div>

    <!-- Bottom Info (Left) -->
    <div class="absolute bottom-[90px] left-0 right-[80px] p-4 flex flex-col gap-2 text-white pointer-events-auto">
      <div class="flex items-center gap-2 mb-1 cursor-pointer">
         <span class="font-bold text-lg drop-shadow-md">@{{ video.uploaderNickname }}</span>
         <!-- <span class="px-2 py-0.5 bg-white/20 text-xs rounded-full backdrop-blur-md">关注</span> -->
      </div>
      <h3 class="font-bold text-base line-clamp-2 drop-shadow-md mb-1">{{ video.title }}</h3>
      <p class="text-sm opacity-90 line-clamp-3 drop-shadow-md leading-relaxed pr-2 text-gray-100">{{ video.description }}</p>
      <div class="flex flex-wrap gap-2 text-xs font-bold mt-1">
         <span v-for="tag in video.tags" :key="tag" class="text-white/80">#{{ tag }}</span>
      </div>
      

    </div>

    <!-- Progress Bar (Draggable) -->
    <div 
      class="absolute bottom-[75px] left-0 right-0 z-40 px-0 group/progress touch-none h-6 flex items-end transition-opacity duration-300 mb-safe"
      :class="(isPlaying && !isDragging) ? 'opacity-0 hover:opacity-100' : 'opacity-100'"
      @touchstart="isDragging = true"
      @touchend="onSeekEnd"
    >
       <!-- Track -->
       <div 
         class="relative w-full bg-white/30 transition-all rounded-full"
         :class="isDragging ? 'h-2' : 'h-0.5 group-hover/progress:h-2'"
       >
          <!-- Buffered/Filled -->
          <div class="absolute left-0 top-0 bottom-0 bg-white/50 rounded-full" :style="{ width: '100%' }"></div>
          <div class="absolute left-0 top-0 bottom-0 bg-indigo-500 rounded-full" :style="{ width: progressPercent + '%' }"></div>
          
          <!-- Slider Input (Big Touch Target) -->
          <input 
            type="range" 
            min="0" 
            :max="duration" 
            step="0.1"
            v-model="currentTime"
            class="absolute -top-4 -bottom-4 left-0 w-full opacity-0 cursor-pointer"
            @input="onSeek"
            @change="onSeekEnd"
            @mousedown="isDragging = true"
            @mouseup="onSeekEnd"
          />
       </div>
    </div>

    <!-- Right Sidebar (Actions) -->
    <div class="absolute bottom-[110px] right-2 flex flex-col items-center gap-5 pointer-events-auto pb-safe">
       <!-- Avatar -->
       <div class="relative mb-3">
         <div class="w-12 h-12 rounded-full border border-white/50 p-0.5 overflow-hidden">
            <img :src="video.uploaderAvatar" class="w-full h-full rounded-full object-cover" />
         </div>
         <div class="absolute -bottom-2 left-1/2 -translate-x-1/2 bg-red-500 rounded-full w-5 h-5 flex items-center justify-center border border-white">
            <Plus class="w-3 h-3 text-white" />
         </div>
       </div>

       <!-- Like -->
       <button @click="toggleLike" class="flex flex-col items-center gap-1 group">
          <Heart class="w-8 h-8 drop-shadow-lg transition-all active:scale-75" :class="isLiked ? 'fill-red-500 text-red-500' : 'text-white'" />
          <span class="text-xs font-semibold text-white drop-shadow-md">{{ formatNumber(video.likeCount) }}</span>
       </button>

       <!-- Comment -->
       <button class="flex flex-col items-center gap-1 group" @click="$emit('open-comments')">
          <MessageCircle class="w-8 h-8 text-white drop-shadow-lg" />
          <span class="text-xs font-semibold text-white drop-shadow-md">{{ formatNumber(video.commentCount) }}</span>
       </button>

       <!-- Star (Favorite) -->
       <button class="flex flex-col items-center gap-1 group">
          <Star class="w-8 h-8 text-white drop-shadow-lg" />
          <span class="text-xs font-semibold text-white drop-shadow-md">收藏</span>
       </button>

       <!-- Share -->
       <button class="flex flex-col items-center gap-1 group">
          <Share2 class="w-8 h-8 text-white drop-shadow-lg" />
          <span class="text-xs font-semibold text-white drop-shadow-md">转发</span>
       </button>
       
       <!-- Vinyl Record (Animation) -->
        <!-- Vinyl Record (Animation) -->
        <button 
           @click.stop="toggleMusicInfo"
           class="mt-4 w-10 h-10 rounded-full bg-[#1f1f1f] border-4 border-[#121212] flex items-center justify-center overflow-hidden animate-[spin_5s_linear_infinite] active:scale-90 transition-transform"
        >
           <img :src="video.uploaderAvatar" class="w-6 h-6 rounded-full" />
        </button>
        
        <!-- Music Note (Floating) -->
        <transition name="fade-slide">
           <div v-if="showMusicInfo" class="absolute bottom-4 right-0 flex justify-end items-end pointer-events-none overflow-visible w-[150px]">
              <div class="flex items-center gap-2 opacity-90 mr-12 mb-2 bg-black/60 px-3 py-1.5 rounded-full backdrop-blur-md shadow-xl border border-white/10">
                <Music class="w-3 h-3 text-white" />
                <div class="overflow-hidden max-w-[100px]">
                  <div class="whitespace-nowrap text-xs text-white animate-marquee">原始声音 - {{ video.uploaderNickname }}</div>
                </div>
              </div>
           </div>
        </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Plus, Heart, MessageCircle, Star, Share2, Play, Music } from 'lucide-vue-next'
import type { VideoVO } from '@/api'

const props = defineProps<{
  video: VideoVO
  isActive: boolean
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const isPlaying = ref(false)
const isLiked = ref((props.video as any).isLiked || false)
const isLoading = ref(false)

const showMusicInfo = ref(false)
let musicInfoTimeout: any = null
const objectFitClass = ref('object-cover') // Default
const duration = ref(0)
const currentTime = ref(0)
const progressPercent = ref(0)
const isDragging = ref(false)

const onLoadStart = () => {
  console.log('[MobileVideoItem] Video load started:', props.video.id)
  isLoading.value = true
}

const onCanPlay = () => {
  console.log('[MobileVideoItem] Video can play:', props.video.id)
  isLoading.value = false
}

const onWaiting = () => {
  console.log('[MobileVideoItem] Video waiting (buffering):', props.video.id)
  isLoading.value = true
}

const onStalled = () => {
  console.warn('[MobileVideoItem] Video stalled (network issue):', props.video.id)
  isLoading.value = true
}

const onMetadataLoaded = () => {
  if (!videoRef.value) return
  const { videoWidth, videoHeight, duration: d } = videoRef.value
  duration.value = d
  
  const ratio = videoWidth / videoHeight
  if (ratio >= 1) {
    objectFitClass.value = 'object-contain'
  } else {
    objectFitClass.value = 'object-cover'
  }
}

const onTimeUpdate = () => {
  if (!videoRef.value || isDragging.value) return
  currentTime.value = videoRef.value.currentTime
  progressPercent.value = (currentTime.value / duration.value) * 100
}

const onSeek = () => {
  isDragging.value = true
  progressPercent.value = (currentTime.value / duration.value) * 100
}

const onSeekEnd = () => {
  if (videoRef.value) {
    videoRef.value.currentTime = currentTime.value
  }
  isDragging.value = false
}

const onVideoError = (event: Event) => {
  const videoElement = event.target as HTMLVideoElement
  const error = videoElement.error
  console.error('[MobileVideoItem] Video error:', {
    videoId: props.video.id,
    videoUrl: props.video.videoUrl,
    errorCode: error?.code,
    errorMessage: error?.message,
    networkState: videoElement.networkState,
    readyState: videoElement.readyState
  })
  
  // If video failed to load and we haven't already set a fallback
  if (error && !props.video.videoUrl?.includes('mdn.mozilla.net')) {
    console.warn('[MobileVideoItem] Video failed to load, using fallback URL for video:', props.video.id)
    // Use a working fallback video
    const fallbackUrl = 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4'
    if (videoRef.value) {
      videoRef.value.src = fallbackUrl
      videoRef.value.load()
      // Update the video object to prevent re-triggering error
      ;(props.video as any).videoUrl = fallbackUrl
    }
  }
}

const toggleMusicInfo = () => {
  showMusicInfo.value = !showMusicInfo.value
  
  if (showMusicInfo.value) {
    clearTimeout(musicInfoTimeout)
    musicInfoTimeout = setTimeout(() => {
      showMusicInfo.value = false
    }, 3000)
  }
}

const togglePlay = () => {
  if (!videoRef.value) return
  if (videoRef.value.paused) {
    playVideo()
  } else {
    pauseVideo()
  }
}

const playVideo = () => {
    if (!videoRef.value) return
    
    // Check if video has a valid source
    if (!props.video.videoUrl || props.video.videoUrl.trim() === '') {
        console.error('[MobileVideoItem] Cannot play video - no valid videoUrl:', props.video)
        return
    }
    
    console.log('[MobileVideoItem] Playing video:', props.video.id, 'URL:', props.video.videoUrl)
    
    videoRef.value.muted = false
    videoRef.value.play().catch((error) => {
        console.log('[MobileVideoItem] Autoplay blocked or error:', error.message)
        if (videoRef.value) {
            videoRef.value.muted = true
            videoRef.value.play().catch((err) => {
                console.error('[MobileVideoItem] Failed to play even when muted:', err.message, 'Video:', props.video.id)
            })
        }
    })
    isPlaying.value = true
}

const pauseVideo = () => {
    if (!videoRef.value) return
    videoRef.value.pause()
    isPlaying.value = false
}

const toggleLike = () => {
  isLiked.value = !isLiked.value
}

const formatNumber = (num: number) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num
}

import { onMounted } from 'vue'

onMounted(() => {
    if (props.isActive) {
        playVideo()
    }
})

watch(() => props.isActive, (active) => {
  if (active) {
    playVideo()
  } else {
    pauseVideo()
    if (videoRef.value) videoRef.value.currentTime = 0
  }
})
</script>
<style scoped>
@keyframes marquee {
  0% { transform: translateX(100%); }
  100% { transform: translateX(-100%); }
}
.animate-marquee {
  display: inline-block;
  animation: marquee 5s linear infinite;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>
