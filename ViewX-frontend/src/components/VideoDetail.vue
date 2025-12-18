<template>
  <div class="fixed inset-0 z-50 bg-[#0f0f0f] flex flex-col md:flex-row text-white overflow-hidden">
    
    <!-- 返回按钮 (Mobile: Top Left, Desktop: Top Left in content area) -->
    <button 
      @click="$emit('close')" 
      class="absolute top-4 left-4 z-[60] p-2 rounded-full bg-black/50 hover:bg-white/20 backdrop-blur-md transition-colors text-white"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
      </svg>
    </button>

    <!-- 主内容区 (视频播放器 / 图片预览) -->
    <div class="flex-1 bg-black relative flex items-center justify-center h-[50vh] md:h-full">
      <div v-if="loading" class="absolute inset-0 flex items-center justify-center">
        <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-500"></div>
      </div>

      <div v-else-if="error" class="text-center p-8">
        <p class="text-red-500 mb-4">{{ error }}</p>
        <button @click="loadContent" class="px-4 py-2 bg-indigo-600 rounded-lg">重试</button>
      </div>

      <div v-else class="w-full h-full flex items-center justify-center relative group/player">
        
        <!-- VIDEO PLAYER -->
        <div v-if="content?.contentType === 'VIDEO'" class="w-full h-full relative">
          <video 
            ref="videoRef"
            :src="content.primaryUrl" 
            class="w-full h-full object-contain"
            @timeupdate="onTimeUpdate"
            @loadedmetadata="onLoadedMetadata"
            @ended="isPlaying = false"
            @click="togglePlay"
          ></video>

          <!-- Video Controls Overlay -->
          <div class="absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-black/80 to-transparent transition-opacity duration-300" 
               :class="isPlaying && !showControls ? 'opacity-0 hover:opacity-100' : 'opacity-100'">
            
            <!-- Progress Bar -->
            <div 
              class="w-full h-1.5 bg-white/20 rounded-full mb-4 cursor-pointer relative group/progress"
              @click="seek"
            >
              <div class="absolute h-full bg-indigo-500 rounded-full" :style="{ width: `${progress}%` }"></div>
              <div class="absolute h-3 w-3 bg-white rounded-full top-1/2 -mt-1.5 -ml-1.5 shadow-md transform scale-0 group-hover/progress:scale-100 transition-transform" :style="{ left: `${progress}%` }"></div>
            </div>

            <!-- Buttons Row -->
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-4">
                <!-- Play/Pause -->
                <button @click="togglePlay" class="text-white hover:text-indigo-400 transition-colors">
                  <component :is="isPlaying ? PauseIcon : PlayIcon" class="w-8 h-8 fill-current" />
                </button>

                <!-- Volume -->
                <div class="flex items-center gap-2 group/volume relative">
                  <button @click="toggleMute" class="text-white hover:text-indigo-400">
                    <component :is="isMuted || volume === 0 ? VolumeXIcon : Volume2Icon" class="w-6 h-6" />
                  </button>
                  <div class="w-0 overflow-hidden group-hover/volume:w-24 transition-all duration-300">
                    <input 
                      type="range" 
                      min="0" 
                      max="1" 
                      step="0.1" 
                      v-model.number="volume" 
                      @input="updateVolume"
                      class="w-20 h-1 bg-white/20 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                    />
                  </div>
                </div>

                <!-- Time -->
                <span class="text-xs text-gray-300 font-mono">
                  {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
                </span>
              </div>

              <div class="flex items-center gap-4">
                <!-- Playback Speed -->
                <div class="relative group/speed">
                  <button class="text-sm font-medium hover:text-indigo-400 px-2 py-1 rounded bg-white/5 hover:bg-white/10 transition-colors">
                    {{ playbackRate }}x
                  </button>
                  <div class="absolute bottom-full right-0 mb-2 bg-[#1a1a1a] border border-white/10 rounded-lg overflow-hidden hidden group-hover/speed:block min-w-[80px]">
                    <button 
                      v-for="rate in [0.5, 1.0, 1.25, 1.5, 2.0]" 
                      :key="rate" 
                      @click="setPlaybackRate(rate)"
                      class="block w-full text-left px-4 py-2 text-sm hover:bg-white/10"
                      :class="playbackRate === rate ? 'text-indigo-500 font-bold' : 'text-gray-300'"
                    >
                      {{ rate }}x
                    </button>
                  </div>
                </div>

                <!-- Fullscreen -->
                <button @click="toggleFullscreen" class="text-white hover:text-indigo-400">
                   <MaximizeIcon class="w-5 h-5" />
                </button>
              </div>
            </div>
          </div>
          
          <!-- Center Play Button (Initial/Paused) -->
          <div v-if="!isPlaying" class="absolute inset-0 flex items-center justify-center pointer-events-none">
             <div class="w-20 h-20 rounded-full bg-black/40 backdrop-blur-sm flex items-center justify-center border border-white/20">
                <PlayIcon class="w-10 h-10 ml-1 text-white fill-white" />
             </div>
          </div>
        </div>

        <!-- IMAGE PLAYER -->
        <div v-else-if="content?.contentType === 'IMAGE'" class="w-full h-full flex items-center justify-center p-4">
           <img :src="content.primaryUrl" class="max-w-full max-h-full object-contain rounded-lg shadow-2xl" />
        </div>

        <!-- IMAGE SET PLAYER -->
        <div v-else-if="content?.contentType === 'IMAGE_SET'" class="w-full h-full relative group/gallery">
           <!-- Main Image -->
           <div class="w-full h-full flex items-center justify-center p-4">
              <img :src="currentImageSetUrl" class="max-w-full max-h-full object-contain rounded-lg shadow-2xl transition-opacity duration-300" />
           </div>

           <!-- Navigation Arrows -->
           <button 
             v-if="imageSetIndex > 0"
             @click="prevImage" 
             class="absolute left-4 top-1/2 -translate-y-1/2 p-3 rounded-full bg-black/50 hover:bg-white/20 backdrop-blur text-white opacity-0 group-hover/gallery:opacity-100 transition-all"
           >
             <ChevronLeftIcon class="w-8 h-8" />
           </button>
           <button 
             v-if="content.mediaUrls && imageSetIndex < content.mediaUrls.length - 1"
             @click="nextImage" 
             class="absolute right-4 top-1/2 -translate-y-1/2 p-3 rounded-full bg-black/50 hover:bg-white/20 backdrop-blur text-white opacity-0 group-hover/gallery:opacity-100 transition-all"
           >
             <ChevronRightIcon class="w-8 h-8" />
           </button>

           <!-- Indicators / Thumbs -->
           <div class="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2 overflow-x-auto max-w-[90%] p-2 rounded-xl bg-black/40 backdrop-blur-md">
              <button 
                v-for="(url, idx) in content.mediaUrls" 
                :key="idx"
                @click="imageSetIndex = idx"
                class="w-12 h-12 rounded-lg border-2 overflow-hidden transition-all shrink-0"
                :class="imageSetIndex === idx ? 'border-indigo-500 scale-110' : 'border-transparent opacity-60 hover:opacity-100'"
              >
                <img :src="url" class="w-full h-full object-cover" />
              </button>
           </div>
        </div>

      </div>
    </div>

    <!-- 侧边栏信息区 -->
    <div class="w-full md:w-[400px] bg-[#1a1a1a] border-l border-white/5 flex flex-col h-[50vh] md:h-full z-20 shadow-2xl">
      <div v-if="content" class="flex-1 overflow-y-auto p-6 scrollbar-thin">
        <!-- Author Info -->
        <div class="flex items-center justify-between mb-6">
          <div class="flex items-center gap-3">
             <div class="w-10 h-10 rounded-full bg-gray-700 overflow-hidden border border-white/10">
                <img :src="content.uploaderAvatar" class="w-full h-full object-cover" />
             </div>
             <div>
                <h3 class="font-bold text-white text-sm">{{ content.uploaderNickname }}</h3>
                <span class="text-xs text-gray-400">Published on {{ formatDate(content.publishedAt) }}</span>
             </div>
          </div>
          <button 
            @click="toggleFollow" 
            class="px-4 py-1.5 rounded-full text-xs font-bold transition-all"
            :class="content.isFollowingUploader ? 'bg-white/10 text-gray-300' : 'bg-indigo-600 text-white hover:bg-indigo-700'"
          >
            {{ content.isFollowingUploader ? '已关注' : '关注' }}
          </button>
        </div>

        <!-- Title & Desc -->
        <h1 class="text-xl font-bold text-white mb-2 leading-snug">{{ content.title }}</h1>
        <div class="flex flex-wrap gap-2 mb-4" v-if="content.tags && content.tags.length">
           <span v-for="tag in content.tags" :key="tag" class="text-xs text-indigo-400">#{{ tag }}</span>
        </div>
        <p class="text-sm text-gray-300 leading-relaxed whitespace-pre-wrap mb-6 border-b border-white/5 pb-6">
           {{ content.description || '暂无描述' }}
        </p>

        <!-- Interactions -->
        <div class="flex items-center justify-between mb-8">
           <button 
             @click="toggleLike"
             class="flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors"
             :class="{'text-pink-500 bg-pink-500/10': content.isLiked}"
           >
             <HeartIcon class="w-5 h-5" :class="{'fill-current': content.isLiked}" />
             <span>{{ formatNumber(content.likeCount) }}</span>
           </button>

           <button 
             @click="toggleFavorite"
             class="flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors"
             :class="{'text-yellow-500 bg-yellow-500/10': content.isFavorited}"
           >
             <BookmarkIcon class="w-5 h-5" :class="{'fill-current': content.isFavorited}" />
             <span>收藏</span>
           </button>

           <button class="flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors">
             <Share2Icon class="w-5 h-5" />
             <span>分享</span>
           </button>
        </div>

        <!-- Stats Grid -->
        <div class="grid grid-cols-2 gap-4 p-4 bg-white/5 rounded-xl border border-white/5 mb-6">
           <div class="text-center">
              <div class="text-xs text-gray-500 mb-1">观看</div>
              <div class="text-lg font-bold">{{ formatNumber(content.viewCount) }}</div>
           </div>
           <div class="text-center">
              <div class="text-xs text-gray-500 mb-1">评论</div>
              <div class="text-lg font-bold">{{ formatNumber(content.commentCount) }}</div>
           </div>
        </div>

        <!-- Comments Section -->
        <div>
           <h3 class="font-bold mb-4">评论 ({{ formatNumber(totalComments) }})</h3>
           
           <!-- Comment Input -->
           <div class="mb-6 flex gap-3">
              <div class="w-8 h-8 rounded-full bg-gray-700 shrink-0 overflow-hidden">
                 <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=User" class="w-full h-full object-cover" />
              </div>
              <div class="flex-1 flex gap-2">
                 <input 
                   v-model="commentInput"
                   type="text" 
                   placeholder="留下你的精彩评论..." 
                   class="flex-1 bg-white/5 rounded-full px-4 py-2 text-sm text-white placeholder-gray-500 focus:outline-none focus:bg-white/10 transition-colors"
                   @keyup.enter="submitComment"
                 />
                 <button 
                   @click="submitComment"
                   :disabled="!commentInput.trim() || submittingComment || !content?.id"
                   class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-700 disabled:cursor-not-allowed rounded-full text-sm font-bold transition-colors"
                 >
                    发送
                 </button>
              </div>
           </div>

           <!-- Comments List -->
           <div v-if="loadingComments" class="text-center py-8 text-gray-500 text-sm">
              加载评论中...
           </div>
           <div v-else-if="comments.length === 0" class="text-center py-8 text-gray-500 text-sm bg-white/5 rounded-xl border border-white/5">
              暂无评论，快来抢沙发
           </div>
           <div v-else class="space-y-4">
              <div v-for="comment in comments" :key="comment.id" class="flex gap-3">
                 <!-- Avatar -->
                 <div class="w-8 h-8 rounded-full overflow-hidden shrink-0 border border-white/10">
                    <img :src="comment.avatar" class="w-full h-full object-cover" />
                 </div>
                 
                 <div class="flex-1">
                    <!-- User & Time -->
                    <div class="flex items-center gap-2 mb-1">
                       <span class="text-xs font-bold text-gray-300">{{ comment.nickname || comment.username }}</span>
                       <span class="text-[10px] text-gray-500">{{ formatCommentTime(comment.createdAt) }}</span>
                    </div>
                    
                    <!-- Content -->
                    <p class="text-sm text-gray-200 leading-relaxed whitespace-pre-wrap">{{ comment.content }}</p>
                    
                    <!-- Replies -->
                    <div v-if="comment.replies && comment.replies.length > 0" class="mt-3 space-y-2 pl-3 border-l-2 border-white/10">
                       <div v-for="reply in comment.replies" :key="reply.id" class="flex gap-2">
                          <div class="w-6 h-6 rounded-full overflow-hidden shrink-0">
                             <img :src="reply.avatar" class="w-full h-full object-cover" />
                          </div>
                          <div class="flex-1">
                             <div class="flex items-center gap-2 mb-0.5">
                                <span class="text-xs font-bold text-gray-400">{{ reply.nickname || reply.username }}</span>
                                <span class="text-[10px] text-gray-500">{{ formatCommentTime(reply.createdAt) }}</span>
                             </div>
                             <p class="text-sm text-gray-300">{{ reply.content }}</p>
                          </div>
                       </div>
                    </div>
                 </div>
              </div>
           </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { contentApi, interactionApi, videoApi, commentApi, type ContentDetailVO, type VideoVO, type CommentVO } from '@/api'
import { 
  Play as PlayIcon, 
  Pause as PauseIcon, 
  Volume2 as Volume2Icon, 
  VolumeX as VolumeXIcon,
  Maximize as MaximizeIcon,
  Heart as HeartIcon, 
  Bookmark as BookmarkIcon, 
  Share2 as Share2Icon,
  ChevronLeft as ChevronLeftIcon,
  ChevronRight as ChevronRightIcon
} from 'lucide-vue-next'

const props = defineProps<{
  video: VideoVO | null
}>()

const emit = defineEmits(['close'])

const loading = ref(true)
const error = ref('')
const content = ref<ContentDetailVO | null>(null)

// Player State
const videoRef = ref<HTMLVideoElement | null>(null)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const progress = ref(0)
const volume = ref(1)
const isMuted = ref(false)
const playbackRate = ref(1.0)
const showControls = ref(true)
const isDragging = ref(false)

// Image Set State
const imageSetIndex = ref(0)

// Comments State
const comments = ref<CommentVO[]>([])
const loadingComments = ref(false)
const submittingComment = ref(false)
const commentInput = ref('')
const totalComments = ref(0)

const currentImageSetUrl = computed(() => {
  if (content.value?.contentType === 'IMAGE_SET' && content.value.mediaUrls) {
    return content.value.mediaUrls[imageSetIndex.value]
  }
  return ''
})

// === Fetch Comments ===
const loadComments = async () => {
  if (!props.video?.id) return
  loadingComments.value = true
  try {
    const res = await commentApi.getComments(props.video.id)
    comments.value = res || []
    
    // Calculate total
    let count = comments.value.length
    comments.value.forEach(c => {
      if (c.replies) count += c.replies.length
    })
    totalComments.value = count
  } catch (e) {
    console.error('Failed to load comments', e)
  } finally {
    loadingComments.value = false
  }
}

// === Submit Comment ===
const submitComment = async () => {
  if (!commentInput.value.trim() || submittingComment.value || !content.value?.id) return
  
  submittingComment.value = true
  try {
    await commentApi.addComment(content.value.id, commentInput.value)
    commentInput.value = ''
    await loadComments() // Reload comments
  } catch (e) {
    console.error('Failed to submit comment', e)
  } finally {
    submittingComment.value = false
  }
}

// === Format Comment Time ===
const formatCommentTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  
  return `${date.getMonth() + 1}-${date.getDate()}`
}

// === Fetch Data ===
const loadContent = async () => {
  if (!props.video) return
  
  loading.value = true
  error.value = ''
  
  try {
    // 优先尝试从新内容系统获取
    try {
      // 传入 skipErrorHandler: true 以避免全局错误提示干扰用户（因为 404 是预期的兼容性流程）
      // 同时通过 headers 和 params 传递以防止 axios 过滤或 header 类型不兼容
      const res = await contentApi.getContentDetail(props.video.id, { 
        skipErrorHandler: true,
        headers: { 'X-Skip-Error-Handler': 'true' },
        params: { '_skip_error': 'true' }
      })
      content.value = res
    } catch (err: any) {
      // 静默失败：404 说明是旧版视频数据，直接降级尝试从 videoApi 获取
      const videoRes = await videoApi.getDetail(props.video.id)
      
      // 适配旧版数据结构到新版 ContentDetailVO
      content.value = {
        ...videoRes,
        contentType: 'VIDEO',
        primaryUrl: videoRes.videoUrl, // 映射 videoUrl -> primaryUrl
        mediaUrls: [],
        status: 'PUBLISHED', // 默认状态
        visibility: 'PUBLIC' // 默认可见性
      } as unknown as ContentDetailVO
    }
  } catch (err: any) {
    console.error(err)
    error.value = '加载内容失败'
  } finally {
    loading.value = false
  }
}

watch(() => props.video, (newVal) => {
  if (newVal) {
    loadContent()
    loadComments()
  } else {
    content.value = null
    comments.value = []
  }
}, { immediate: true })

// === Video Controls ===
const togglePlay = () => {
  if (!videoRef.value) return
  if (videoRef.value.paused) {
    videoRef.value.play()
    isPlaying.value = true
  } else {
    videoRef.value.pause()
    isPlaying.value = false
  }
}

const onTimeUpdate = () => {
  if (!videoRef.value || isDragging.value) return
  currentTime.value = videoRef.value.currentTime
  progress.value = (currentTime.value / duration.value) * 100
}

const onLoadedMetadata = () => {
  if (!videoRef.value) return
  duration.value = videoRef.value.duration
}

const seek = (event: MouseEvent) => {
  if (!videoRef.value) return
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const x = event.clientX - rect.left
  const percent = x / rect.width
  const newTime = percent * duration.value
  videoRef.value.currentTime = newTime
  currentTime.value = newTime
  progress.value = percent * 100
}

const updateVolume = () => {
  if (!videoRef.value) return
  videoRef.value.volume = volume.value
  isMuted.value = volume.value === 0
}

const toggleMute = () => {
  if (!videoRef.value) return
  if (isMuted.value) {
    volume.value = 1
    videoRef.value.volume = 1
    isMuted.value = false
  } else {
    volume.value = 0
    videoRef.value.volume = 0
    isMuted.value = true
  }
}

const setPlaybackRate = (rate: number) => {
  if (!videoRef.value) return
  videoRef.value.playbackRate = rate
  playbackRate.value = rate
}

const toggleFullscreen = () => {
  const container = videoRef.value?.parentElement
  if (!container) return
  if (!document.fullscreenElement) {
    container.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// === Image Set Controls ===
const prevImage = () => {
  if (imageSetIndex.value > 0) {
    imageSetIndex.value--
  }
}

const nextImage = () => {
  if (content.value?.mediaUrls && imageSetIndex.value < content.value.mediaUrls.length - 1) {
    imageSetIndex.value++
  }
}

// === Interaction ===
const toggleLike = async () => {
  if (!content.value) return
  try {
    await interactionApi.toggleLike(content.value.id)
    content.value.isLiked = !content.value.isLiked
    content.value.likeCount += content.value.isLiked ? 1 : -1
  } catch (e) {
    console.error(e)
  }
}

const toggleFavorite = async () => {
  if (!content.value) return
  try {
    await interactionApi.toggleFavorite(content.value.id)
    content.value.isFavorited = !content.value.isFavorited
  } catch (e) {
    console.error(e)
  }
}

const toggleFollow = () => {
  if (!content.value) return
  // TODO: Call follow API
  content.value.isFollowingUploader = !content.value.isFollowingUploader
}

// === Helpers ===
const formatTime = (seconds: number) => {
  if (!seconds || isNaN(seconds)) return '0:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

const formatNumber = (num: number) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num
}

</script>

<style scoped>
.scrollbar-thin::-webkit-scrollbar {
  width: 6px;
}
.scrollbar-thin::-webkit-scrollbar-track {
  background: transparent;
}
.scrollbar-thin::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}
.scrollbar-thin::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* Custom range slider styling */
input[type=range]::-webkit-slider-thumb {
  -webkit-appearance: none;
  height: 12px;
  width: 12px;
  border-radius: 50%;
  background: white;
  margin-top: -4px;
  cursor: pointer;
}
input[type=range]::-moz-range-thumb {
  height: 12px;
  width: 12px;
  border-radius: 50%;
  background: white;
  cursor: pointer;
  border: none;
}
</style>
