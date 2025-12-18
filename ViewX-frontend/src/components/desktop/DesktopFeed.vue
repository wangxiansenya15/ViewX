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
                    <DesktopVideoItem 
                       :video="video" 
                       :isActive="currentIndex === index"
                       :isFirst="index === 0"
                       :isLast="index === videos.length - 1"
                       force-contain
                       @open-comments="openComments"
                       @prev="scrollToIndex(currentIndex - 1)"
                       @next="scrollToIndex(currentIndex + 1)"
                    />
                </div>
              </div>
           </div>
       </div>

       <!-- Desktop Navigation Controls (Removed, moved to VideoItem) -->
    </div>

    <!-- Right Side Comment Panel -->
    <transition name="slide-right">
        <div v-if="showComments" class="w-[400px] h-full border-l border-white/10 bg-[#161616] shrink-0 z-20">
            <DesktopCommentPanel 
                :videoId="activeVideoId"
                @close="showComments = false" 
            />
        </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import DesktopVideoItem from './DesktopVideoItem.vue'
import DesktopCommentPanel from './DesktopCommentPanel.vue'
import type { VideoVO } from '@/api'

const props = defineProps<{
  videos: VideoVO[]
}>()

const activeVideoId = computed(() => {
    const id = (props.videos.length > 0 && currentIndex.value >= 0 && currentIndex.value < props.videos.length) 
        ? props.videos[currentIndex.value].id 
        : 0
    console.log('[DesktopFeed] activeVideoId:', id, 'currentIndex:', currentIndex.value, 'videos.length:', props.videos.length)
    return id
})

const emit = defineEmits(['load-more'])

const containerRef = ref<HTMLElement | null>(null)
const currentIndex = ref(0)
const showComments = ref(false)
const loadMoreTriggered = ref(false)
let scrollTimeout: any = null

// 保存滚动位置
let savedScrollTop = 0
let savedCurrentIndex = 0

const handleScroll = () => {
  if (!containerRef.value) return
  
  clearTimeout(scrollTimeout)
  scrollTimeout = setTimeout(() => {
    const scrollTop = containerRef.value!.scrollTop
    const clientHeight = containerRef.value!.clientHeight
    const index = Math.round(scrollTop / clientHeight)
    
    // 实时保存滚动位置
    savedScrollTop = scrollTop
    savedCurrentIndex = index
    
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

const openComments = () => {
  console.log('[DesktopFeed] Opening comments, activeVideoId:', activeVideoId.value, 'videos:', props.videos.length)
  if (activeVideoId.value === 0) {
    console.warn('[DesktopFeed] Cannot open comments: videoId is 0')
    alert('视频还在加载中，请稍后再试')
    return
  }
  showComments.value = true
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

// 保存滚动位置方法
const saveScrollPosition = () => {
  console.log('🔴 [DesktopFeed] saveScrollPosition - scrollTop:', savedScrollTop, 'index:', savedCurrentIndex)
}

// 恢复滚动位置方法
const restoreScrollPosition = () => {
  console.log('🟢 [DesktopFeed] RESTORING - savedScrollTop:', savedScrollTop, 'savedIndex:', savedCurrentIndex)
  
  if (!containerRef.value) {
    console.warn('🟢 [DesktopFeed] Cannot restore - containerRef is null')
    return
  }
  
  // 临时移除 scroll-smooth 类
  const container = containerRef.value
  const hadSmoothScroll = container.classList.contains('scroll-smooth')
  
  if (hadSmoothScroll) {
    container.classList.remove('scroll-smooth')
  }
  
  // 立即设置滚动位置，不等待 requestAnimationFrame
  containerRef.value.scrollTop = savedScrollTop
  currentIndex.value = savedCurrentIndex
  console.log('🟢 [DesktopFeed] RESTORED - scrollTop:', containerRef.value.scrollTop, 'currentIndex:', currentIndex.value)
  
  // 恢复 scroll-smooth 类
  if (hadSmoothScroll) {
    setTimeout(() => {
      if (containerRef.value) {
        containerRef.value.classList.add('scroll-smooth')
      }
    }, 100)
  }
}

onMounted(() => {
    window.addEventListener('keydown', handleKeydown)
})

// 暴露方法给父组件
defineExpose({
  saveScrollPosition,
  restoreScrollPosition
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
