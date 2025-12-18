<template>
  <div class="relative w-full h-full bg-black snap-start snap-always shrink-0 overflow-hidden group">
    <!-- Video Player with Controls -->
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
      controls
      controlsList="nodownload"
      @loadedmetadata="onMetadataLoaded"
      @error="onVideoError"
      @play="isPlaying = true"
      @pause="isPlaying = false"
    ></video>
    
    <!-- Loading Indicator -->
    <div v-if="isLoading" class="absolute inset-0 flex items-center justify-center bg-black/50 z-20 pointer-events-none">
      <div class="flex flex-col items-center gap-3">
        <div class="w-12 h-12 border-4 border-white/30 border-t-white rounded-full animate-spin"></div>
        <div class="text-white text-sm">加载中...</div>
      </div>
    </div>
    
    <!-- Play/Pause Overlay (Center) -->
    <div 
      v-if="!isPlaying"
      class="absolute inset-0 flex items-center justify-center z-20 cursor-pointer pointer-events-none"
    >
      <div class="w-20 h-20 bg-black/40 rounded-full flex items-center justify-center backdrop-blur-sm transition-transform hover:scale-110">
        <Play class="w-10 h-10 text-white fill-white ml-1.5" />
      </div>
    </div>
    
    <!-- Video Background (for containment) -->
    <div v-if="objectFitClass === 'object-contain'" class="absolute inset-0 z-[-1]">
       <img :src="video.thumbnailUrl" class="w-full h-full object-cover blur-3xl opacity-50" />
    </div>

    <!-- Bottom Gradient Overlay -->
    <div class="absolute inset-x-0 bottom-0 h-40 bg-gradient-to-t from-black/80 via-black/40 to-transparent pointer-events-none z-10"></div>

    <!-- Bottom Info (Title, Desc, User) -->
    <div class="absolute bottom-20 left-4 right-[80px] z-30 flex flex-col items-start gap-3 pointer-events-none">
       <!-- Author Name -->
       <div class="pointer-events-auto cursor-pointer font-bold text-white text-2xl drop-shadow-md hover:underline transition-all" @click.stop="goToProfile">
         @{{ video.uploaderNickname }}
       </div>
       
       <!-- Title & Description -->
       <div class="pointer-events-auto text-left space-y-2">
          <div v-if="video.title" class="text-white text-lg font-medium leading-relaxed drop-shadow-md">{{ video.title }}</div>
          <p v-if="video.description" class="text-white/95 text-base line-clamp-2 font-normal leading-snug drop-shadow-md">{{ video.description }}</p>
       </div>
    </div>

    <!-- Right Sidebar (Actions) -->
    <div class="absolute bottom-[120px] right-2 flex flex-col items-center gap-5 pointer-events-auto pb-safe z-40">
       <!-- Navigation Buttons -->
       <div class="flex flex-col gap-4 mb-2">
           <button 
             @click.stop="$emit('prev')" 
             :disabled="isFirst"
             class="p-2 bg-white/10 hover:bg-white/20 rounded-full backdrop-blur-md text-white disabled:opacity-30 disabled:cursor-not-allowed transition-all"
           >
              <ChevronUp class="w-6 h-6" />
           </button>
           <button 
             @click.stop="$emit('next')" 
             :disabled="isLast"
             class="p-2 bg-white/10 hover:bg-white/20 rounded-full backdrop-blur-md text-white disabled:opacity-30 disabled:cursor-not-allowed transition-all"
           >
              <ChevronDown class="w-6 h-6" />
           </button>
       </div>

       <!-- Avatar -->
       <div class="relative mb-3 group active:scale-95 transition-transform" @click.stop="goToProfile">
         <div class="w-12 h-12 rounded-full border border-white/50 p-0.5 overflow-hidden">
            <img :src="video.uploaderAvatar" class="w-full h-full rounded-full object-cover" />
         </div>
         <div v-if="!isFollowing" @click.stop="toggleFollow" class="absolute -bottom-2 left-1/2 -translate-x-1/2 bg-red-500 rounded-full w-5 h-5 flex items-center justify-center border border-white transition-all hover:scale-110 cursor-pointer">
            <Plus class="w-3 h-3 text-white" />
         </div>
         <div v-else class="absolute -bottom-2 left-1/2 -translate-x-1/2 bg-white/20 backdrop-blur-md rounded-full w-5 h-5 flex items-center justify-center border border-white/50 transition-all opacity-0 group-hover:opacity-100">
             <Check class="w-3 h-3 text-white" />
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { Plus, Heart, MessageCircle, Star, Share2, Check, ChevronUp, ChevronDown, Play } from 'lucide-vue-next'
import { interactionApi, type VideoVO } from '@/api'
import { useRouter } from 'vue-router'

const props = defineProps<{
  video: VideoVO
  isActive: boolean
  forceContain?: boolean
  isFirst?: boolean
  isLast?: boolean
}>()

const emit = defineEmits<{
  (e: 'open-comments'): void
  (e: 'prev'): void
  (e: 'next'): void
}>()

const router = useRouter()
const videoRef = ref<HTMLVideoElement | null>(null)
const isPlaying = ref(false)
const isLoading = ref(false)

// Interaction States
const isLiked = ref((props.video as any).isLiked || false)
const isFavorited = ref((props.video as any).isFavorited || false)
const isFollowing = ref(false)

const objectFitClass = ref(props.forceContain ? 'object-contain' : 'object-cover') 
const videoStyle = ref<Record<string, string>>({})

// Fetch interaction status
const fetchInteractionStatus = async () => {
    try {
        const res = await interactionApi.getStatus(props.video.id)
        isLiked.value = res.liked
        isFavorited.value = res.favorited
        
        if (props.video.uploaderId) {
             const followRes = await interactionApi.getDetailedFollowStatus(props.video.uploaderId)
             isFollowing.value = followRes.isFollowing
        }
    } catch (e) {
        // Silent fail
    }
}

const goToProfile = () => {
    if (props.video.uploaderId) {
        router.push(`/profile/${props.video.uploaderId}`)
    }
}

const toggleFollow = async () => {
    try {
        await interactionApi.toggleFollow(props.video.uploaderId)
        isFollowing.value = !isFollowing.value
    } catch (e) {
        console.error('Follow failed', e)
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
  }
})

const playVideo = () => {
    if (!videoRef.value) return
    if (!props.video.videoUrl) return
    
    videoRef.value.play()
      .catch(e => {
        console.error('Play failed', e)
      })
    isPlaying.value = true
}

const pauseVideo = () => {
    if (!videoRef.value) return
    videoRef.value.pause()
    isPlaying.value = false
}

const toggleLike = async () => {
    const previousState = isLiked.value
    isLiked.value = !isLiked.value
    props.video.likeCount += isLiked.value ? 1 : -1
    
    try {
        await interactionApi.toggleLike(props.video.id)
    } catch (e) {
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

const onMetadataLoaded = () => {
  if (!videoRef.value) return
  const { videoWidth, videoHeight } = videoRef.value
  
  const ratio = videoWidth / videoHeight
  
  if (props.forceContain) {
      objectFitClass.value = 'object-contain'
      
      if (ratio < 1) {
          videoStyle.value = {
              width: '100%',
              height: '100%',
              maxWidth: '560px',
              margin: '0 auto'
          }
      } else {
          videoStyle.value = { width: '100%', height: '100%' }
      }
  } else {
      objectFitClass.value = (ratio >= 1) ? 'object-contain' : 'object-cover'
      videoStyle.value = {}
  }
}

const onVideoError = (event: Event) => {
  const videoElement = event.target as HTMLVideoElement
  const error = videoElement.error
  
  if (error && error.code !== 20) {
      console.warn('[DesktopVideoItem] Video error:', {
        videoId: props.video.id,
        code: error.code,
        message: error.message
      })
  }
  
  if (error && !props.video.videoUrl?.includes('mdn.mozilla.net')) {
    const fallbackUrl = 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4'
    if (videoRef.value && videoRef.value.src !== fallbackUrl) {
      console.log('Using fallback video')
      videoRef.value.src = fallbackUrl
      videoRef.value.load()
    }
  }
}

const formatNumber = (num: number) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num
}
</script>
