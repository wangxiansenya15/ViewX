<template>
  <div class="relative w-full h-full bg-black snap-start snap-always shrink-0 overflow-hidden group">
    <!-- Video Player -->
    <video 
      ref="videoRef"
      :src="video.videoUrl"
      class="w-full h-full relative z-0 transition-all duration-300"
      :class="objectFitClass"
      :style="videoStyle"
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

    <!-- Heart Animations (Double Tap) -->
    <div class="absolute inset-0 pointer-events-none z-30 overflow-hidden">
        <transition-group name="heart-pop">
           <div 
             v-for="heart in heartAnimations" 
             :key="heart.id"
             class="absolute w-24 h-24 -ml-12 -mt-12"
             :style="{ left: heart.x + 'px', top: heart.y + 'px', transform: `rotate(${heart.angle}deg)` }"
           >
              <Heart class="w-full h-full text-red-500 fill-red-500 drop-shadow-lg" />
           </div>
        </transition-group>
    </div>

    <!-- Play Overlay (Center) - Only show when paused and not dragging -->
    <div v-if="!isPlaying && !isDragging" class="absolute inset-0 flex items-center justify-center pointer-events-none z-10 transition-opacity duration-200">
      <div class="w-16 h-16 bg-white/20 rounded-full flex items-center justify-center backdrop-blur-sm animate-pulse">
        <Play class="w-8 h-8 text-white fill-white ml-1" />
      </div>
    </div>

    <!-- Right Sidebar (Actions) -->
    <div class="absolute bottom-[110px] right-2 flex flex-col items-center gap-5 pointer-events-auto pb-safe z-40">
       <!-- Avatar -->
       <div class="relative mb-3 group active:scale-95 transition-transform" @click.stop>
         <div class="w-12 h-12 rounded-full border border-white/50 p-0.5 overflow-hidden">
            <img :src="video.uploaderAvatar" class="w-full h-full rounded-full object-cover" />
         </div>
         <div class="absolute -bottom-2 left-1/2 -translate-x-1/2 bg-red-500 rounded-full w-5 h-5 flex items-center justify-center border border-white">
            <Plus class="w-3 h-3 text-white" />
         </div>
       </div>

       <!-- Like -->
       <button @click.stop="toggleLike" class="flex flex-col items-center gap-1 group active:scale-90 transition-transform">
          <Heart class="w-8 h-8 drop-shadow-xl transition-all duration-300" :class="isLiked ? 'fill-red-500 text-red-500 scale-110' : 'text-white'" />
          <span class="text-xs font-semibold text-white drop-shadow-md">{{ formatNumber(video.likeCount) }}</span>
       </button>

       <!-- Comment -->
       <button class="flex flex-col items-center gap-1 group active:scale-90 transition-transform" @click.stop="$emit('open-comments')">
          <MessageCircle class="w-8 h-8 text-white drop-shadow-xl" />
          <span class="text-xs font-semibold text-white drop-shadow-md">{{ formatNumber(video.commentCount) }}</span>
       </button>

       <!-- Star (Favorite) -->
       <button @click.stop="toggleFavorite" class="flex flex-col items-center gap-1 group active:scale-90 transition-transform">
          <Star class="w-8 h-8 drop-shadow-xl transition-all duration-300" :class="isFavorited ? 'fill-yellow-500 text-yellow-500 scale-110' : 'text-white'" />
          <span class="text-xs font-semibold text-white drop-shadow-md">{{ isFavorited ? '已收藏' : '收藏' }}</span>
       </button>

       <!-- Share -->
       <button class="flex flex-col items-center gap-1 group active:scale-90 transition-transform" @click.stop>
          <Share2 class="w-8 h-8 text-white drop-shadow-xl" />
          <span class="text-xs font-semibold text-white drop-shadow-md">转发</span>
       </button>
       
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
import { ref, watch, onMounted } from 'vue'
import { Plus, Heart, MessageCircle, Star, Share2, Play, Music } from 'lucide-vue-next'
import { interactionApi, type VideoVO } from '@/api'

const props = defineProps<{
  video: VideoVO
  isActive: boolean
  forceContain?: boolean
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const isPlaying = ref(false)
const isLoading = ref(false)

// Interaction States
const isLiked = ref((props.video as any).isLiked || false)
const isFavorited = ref((props.video as any).isFavorited || false)

const showMusicInfo = ref(false)
let musicInfoTimeout: any = null
const objectFitClass = ref(props.forceContain ? 'object-contain' : 'object-cover') 
const duration = ref(0)
const currentTime = ref(0)
const progressPercent = ref(0)
const isDragging = ref(false)

// Double Tap Logic
let lastTapTime = 0
const heartAnimations = ref<{id: number, x: number, y: number, angle: number}[]>([])
let heartIdCounter = 0

// Fetch interaction status when active or mounted
const fetchInteractionStatus = async () => {
    try {
        const res = await interactionApi.getStatus(props.video.id)
        isLiked.value = res.liked
        isFavorited.value = res.favorited
    } catch (e) {
        // console.error('Failed to fetch status', e)
    }
}

onMounted(() => {
    if (props.isActive) {
        playVideo()
        fetchInteractionStatus()
    }
})

watch(() => props.isActive, (active) => {
  if (active) {
    playVideo()
    fetchInteractionStatus()
  } else {
    pauseVideo()
    if (videoRef.value) videoRef.value.currentTime = 0
  }
})

// Video Event Handlers
const onLoadStart = () => {
  isLoading.value = true
}

const onCanPlay = () => {
  isLoading.value = false
}

const onWaiting = () => {
  isLoading.value = true
}

const onStalled = () => {
  isLoading.value = true
}

// Double tap handler
// Double tap handler
let singleTapTimer: any = null

const togglePlay = (e: MouseEvent | TouchEvent) => {
    const currentTime = Date.now()
    const tapLength = currentTime - lastTapTime
    
    if (tapLength < 300 && tapLength > 0) {
        // Double Tap detected
        if (singleTapTimer) clearTimeout(singleTapTimer)
        handleDoubleTap(e)
    } else {
        // Single Tap - Wait to confirm it's not a double tap
        if (singleTapTimer) clearTimeout(singleTapTimer)
        
        singleTapTimer = setTimeout(() => {
            if (!videoRef.value) return
            if (videoRef.value.paused) {
                playVideo()
            } else {
                pauseVideo()
            }
            singleTapTimer = null
        }, 300)
    }
    lastTapTime = currentTime
}

const handleDoubleTap = (e: MouseEvent | TouchEvent) => {
    // 1. Show Heart Animation
    let clientX, clientY
    if (e instanceof TouchEvent) {
        clientX = e.touches[0].clientX
        clientY = e.touches[0].clientY
    } else {
        clientX = (e as MouseEvent).clientX
        clientY = (e as MouseEvent).clientY
    }

    const id = heartIdCounter++
    const angle = Math.random() * 40 - 20 // Random angle -20 to 20
    heartAnimations.value.push({
        id,
        x: clientX,
        y: clientY,
        angle
    })

    // Remove heart after animation
    setTimeout(() => {
        heartAnimations.value = heartAnimations.value.filter(h => h.id !== id)
    }, 1000)

    // 2. Like Video (if not already liked)
    if (!isLiked.value) {
        toggleLike()
    }
}

const playVideo = () => {
    if (!videoRef.value) return
    if (!props.video.videoUrl) return
    
    videoRef.value.muted = false
    videoRef.value.play().catch(() => {
        if (videoRef.value) {
            videoRef.value.muted = true
            videoRef.value.play().catch(e => console.error('Play failed', e))
        }
    })
    isPlaying.value = true
}

const pauseVideo = () => {
    if (!videoRef.value) return
    videoRef.value.pause()
    isPlaying.value = false
}

const toggleLike = async () => {
    // Optimistic update
    const previousState = isLiked.value
    isLiked.value = !isLiked.value
    props.video.likeCount += isLiked.value ? 1 : -1
    
    try {
        await interactionApi.toggleLike(props.video.id)
    } catch (e) {
        // Revert on failure
        isLiked.value = previousState
        props.video.likeCount += isLiked.value ? 1 : -1
        console.error('Like failed', e)
    }
}

const toggleFavorite = async () => {
    const previousState = isFavorited.value
    isFavorited.value = !isFavorited.value
    
    try {
        await interactionApi.toggleFavorite(props.video.id)
    } catch (e) {
        isFavorited.value = previousState
        console.error('Favorite failed', e)
    }
}

const videoStyle = ref<Record<string, string>>({})

const onMetadataLoaded = () => {
  if (!videoRef.value) return
  const { videoWidth, videoHeight, duration: d } = videoRef.value
  duration.value = d
  
  const ratio = videoWidth / videoHeight
  
  if (props.forceContain) {
      objectFitClass.value = 'object-contain'
      
      // Adaptive scaling for Desktop Feed
      // If video is vertical (ratio < 1), restrict its max-width to prevent massive upscaling of low-res content on large screens.
      // 560px is roughly a large phone/tablet width.
      if (ratio < 1) {
          videoStyle.value = {
              width: '100%',
              height: '100%',
              maxWidth: '560px',
              margin: '0 auto'
          }
      } else {
          // Horizontal video: allow full width/height adaptation
          videoStyle.value = { width: '100%', height: '100%' }
      }
  } else {
      objectFitClass.value = (ratio >= 1) ? 'object-contain' : 'object-cover'
      videoStyle.value = {}
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
  // Only log detailed error if it's not a generic abort
  if (error && error.code !== 20) {
      console.warn('[MobileVideoItem] Video error:', {
        videoId: props.video.id,
        code: error.code,
        message: error.message
      })
  }
  
  // Fallback logic
  if (error && !props.video.videoUrl?.includes('mdn.mozilla.net')) {
    const fallbackUrl = 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4'
    if (videoRef.value && videoRef.value.src !== fallbackUrl) {
      console.log('Using fallback video')
      videoRef.value.src = fallbackUrl
      videoRef.value.load()
    }
  }
}

const toggleMusicInfo = () => {
  showMusicInfo.value = !showMusicInfo.value
  if (showMusicInfo.value) {
    clearTimeout(musicInfoTimeout)
    musicInfoTimeout = setTimeout(() => showMusicInfo.value = false, 3000)
  }
}

const formatNumber = (num: number) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num
}
</script>
<style scoped>
@keyframes heart-pop {
  0% { transform: scale(0) rotate(var(--rotation, 0deg)); opacity: 0; }
  20% { transform: scale(1.2) rotate(var(--rotation, 0deg)); opacity: 1; }
  100% { transform: scale(1.5) translateY(-100px) rotate(var(--rotation, 0deg)); opacity: 0; }
}

.heart-pop-enter-active {
  animation: heart-pop 0.8s ease-out forwards;
}

/* ... existing styles ... */
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
