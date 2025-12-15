<template>
  <div class="flex w-full h-full bg-black overflow-hidden relative">
    <!-- Main Content Area -->
    <div class="flex-1 h-full relative group">
       <!-- Video Container -->
       <div class="w-full h-full flex justify-center bg-black">
           <div 
             class="h-full w-full relative bg-black shadow-2xl border-x border-white/5"
           >
              <!-- Scroll Container -->
              <div 
                ref="containerRef"
                class="w-full h-full overflow-y-scroll snap-y snap-mandatory no-scrollbar scroll-smooth"
                @scroll="handleScroll"
              >
                <div v-for="(video, index) in videos" :key="video.id" class="w-full h-full snap-start shrink-0">
                    <MobileVideoItem 
                       :video="video" 
                       :isActive="currentIndex === index"
                       force-contain
                       @open-comments="showComments = true" 
                    />
                </div>
              </div>
           </div>
       </div>

       <!-- Desktop Navigation Controls (Floating) -->
       <div class="absolute right-8 bottom-8 flex flex-col gap-4 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
           <button 
             @click="scrollToIndex(currentIndex - 1)" 
             :disabled="currentIndex === 0"
             class="p-3 bg-white/10 hover:bg-white/20 rounded-full backdrop-blur-md text-white disabled:opacity-30 disabled:cursor-not-allowed transition-all"
           >
              <ChevronUp class="w-6 h-6" />
           </button>
           <button 
             @click="scrollToIndex(currentIndex + 1)" 
             :disabled="currentIndex >= videos.length - 1"
             class="p-3 bg-white/10 hover:bg-white/20 rounded-full backdrop-blur-md text-white disabled:opacity-30 disabled:cursor-not-allowed transition-all"
           >
              <ChevronDown class="w-6 h-6" />
           </button>
       </div>
    </div>

    <!-- Right Side Comment Panel -->
    <transition name="slide-right">
        <div v-if="showComments" class="w-[400px] h-full border-l border-white/10 bg-[#161616] shrink-0 z-20">
            <DesktopCommentPanel @close="showComments = false" />
        </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ChevronUp, ChevronDown } from 'lucide-vue-next'
import MobileVideoItem from '../mobile/MobileVideoItem.vue'
import DesktopCommentPanel from './DesktopCommentPanel.vue'
import type { VideoVO } from '@/api'

const props = defineProps<{
  videos: VideoVO[]
}>()

const emit = defineEmits(['load-more'])

const containerRef = ref<HTMLElement | null>(null)
const currentIndex = ref(0)
const showComments = ref(false)
const loadMoreTriggered = ref(false)
let scrollTimeout: any = null

const handleScroll = () => {
  if (!containerRef.value) return
  
  clearTimeout(scrollTimeout)
  scrollTimeout = setTimeout(() => {
    const scrollTop = containerRef.value!.scrollTop
    const clientHeight = containerRef.value!.clientHeight
    const index = Math.round(scrollTop / clientHeight)
    
    if (index !== currentIndex.value && index >= 0 && index < props.videos.length) {
      currentIndex.value = index
    }
    
    // Load more logic
    if (props.videos.length > 0 && index >= props.videos.length - 2 && !loadMoreTriggered.value) {
      loadMoreTriggered.value = true
      emit('load-more')
    }
  }, 50)
}

const scrollToIndex = (index: number) => {
  if (!containerRef.value) return
  if (index < 0 || index >= props.videos.length) return
  
  const clientHeight = containerRef.value.clientHeight
  containerRef.value.scrollTo({
    top: index * clientHeight,
    behavior: 'smooth'
  })
}

// Reset loadMore state
watch(() => props.videos.length, (newLength, oldLength) => {
  if (newLength > oldLength) {
    loadMoreTriggered.value = false
  }
})

// Keyboard navigation
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'ArrowUp') {
    e.preventDefault()
    scrollToIndex(currentIndex.value - 1)
  } else if (e.key === 'ArrowDown') {
    e.preventDefault()
    scrollToIndex(currentIndex.value + 1)
    
    // Auto load more if near end
    if (currentIndex.value >= props.videos.length - 2 && !loadMoreTriggered.value) {
        emit('load-more')
        loadMoreTriggered.value = true
    }
  }
}

onMounted(() => {
    window.addEventListener('keydown', handleKeydown)
})

// Cleanup would be good but currently HomeView is main view.
</script>

<style scoped>
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
}
</style>
