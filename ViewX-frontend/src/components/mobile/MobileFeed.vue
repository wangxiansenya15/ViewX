<template>
  <div class="h-full w-full bg-black overflow-y-scroll snap-y snap-mandatory scroll-smooth no-scrollbar" ref="containerRef" @scroll="handleScroll">
    <div v-for="(video, index) in videos" :key="video.id" class="w-full h-full snap-start" :data-index="index">
      <MobileVideoItem :video="video" :isActive="currentIndex === index" @open-comments="$emit('open-comments', video)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import MobileVideoItem from './MobileVideoItem.vue'
import type { VideoVO } from '@/api'

const props = defineProps<{
  videos: VideoVO[]
}>()

const emit = defineEmits(['open-comments', 'load-more'])

const containerRef = ref<HTMLElement | null>(null)
const currentIndex = ref(0)
const loadMoreTriggered = ref(false)
let scrollTimeout: any = null

// 保存滚动位置
let savedScrollTop = 0
let savedCurrentIndex = 0

const handleScroll = () => {
  if (!containerRef.value) return
  
  // Simple debounce not needed for logic but good for performance
  clearTimeout(scrollTimeout)
  scrollTimeout = setTimeout(() => {
    const scrollTop = containerRef.value!.scrollTop
    const clientHeight = containerRef.value!.clientHeight
    const index = Math.round(scrollTop / clientHeight)
    
    // 实时保存滚动位置 - 这样即使组件被 deactivated 也能保存正确的值
    savedScrollTop = scrollTop
    savedCurrentIndex = index
    
    console.log('[MobileFeed] Scroll - index:', index, 'currentIndex:', currentIndex.value, 'totalVideos:', props.videos.length)
    
    if (index !== currentIndex.value && index >= 0 && index < props.videos.length) {
      currentIndex.value = index
      console.log('[MobileFeed] Index changed to:', index)
    }
    
    // Load more when approaching the end (e.g., last 2 videos)
    if (props.videos.length > 0 && index >= props.videos.length - 2 && !loadMoreTriggered.value) {
      console.log('[MobileFeed] Emitting load-more - index:', index, 'threshold:', props.videos.length - 2)
      loadMoreTriggered.value = true
      emit('load-more')
    }
  }, 50)
}


// Reset loadMoreTriggered when new videos are added
watch(() => props.videos.length, (newLength, oldLength) => {
  if (newLength > oldLength) {
    console.log('[MobileFeed] Videos added, resetting loadMoreTriggered. New length:', newLength)
    loadMoreTriggered.value = false
    
    // Preload next videos
    preloadNextVideos()
  }
})

// Preload next videos for better UX on slow networks
const preloadNextVideos = () => {
  const nextIndex = currentIndex.value + 1
  const preloadCount = 2 // Preload next 2 videos
  
  for (let i = nextIndex; i < Math.min(nextIndex + preloadCount, props.videos.length); i++) {
    const video = props.videos[i]
    if (video?.videoUrl) {
      const link = document.createElement('link')
      link.rel = 'preload'
      link.as = 'video'
      link.href = video.videoUrl
      document.head.appendChild(link)
      console.log('[MobileFeed] Preloading video:', i, video.videoUrl)
    }
  }
}

// Watch current index to preload next videos
watch(currentIndex, () => {
  preloadNextVideos()
})

// 暴露方法给父组件
const saveScrollPosition = () => {
  // 滚动位置已经在 handleScroll 中实时保存了,这里只是记录日志
  console.log('🔴 [MobileFeed] saveScrollPosition called - already saved in handleScroll - scrollTop:', savedScrollTop, 'index:', savedCurrentIndex)
}

const restoreScrollPosition = () => {
  console.log('🟢 [MobileFeed] RESTORING - savedScrollTop:', savedScrollTop, 'savedIndex:', savedCurrentIndex)
  
  if (!containerRef.value) {
    console.warn('🟢 [MobileFeed] Cannot restore - containerRef is null')
    return
  }
  
  console.log('🟢 [MobileFeed] Container found, current scrollTop:', containerRef.value.scrollTop)
  
  // 临时移除 scroll-smooth 类,避免滚动动画
  const container = containerRef.value
  const hadSmoothScroll = container.classList.contains('scroll-smooth')
  
  if (hadSmoothScroll) {
    container.classList.remove('scroll-smooth')
    console.log('🟢 [MobileFeed] Removed scroll-smooth for instant jump')
  }
  
  // 使用 requestAnimationFrame 确保在下一帧渲染时执行
  requestAnimationFrame(() => {
    if (containerRef.value) {
      console.log('🟢 [MobileFeed] Setting scrollTop from', containerRef.value.scrollTop, 'to', savedScrollTop)
      containerRef.value.scrollTop = savedScrollTop
      currentIndex.value = savedCurrentIndex
      console.log('🟢 [MobileFeed] RESTORED - actual scrollTop:', containerRef.value.scrollTop, 'currentIndex:', currentIndex.value)
      
      // 恢复 scroll-smooth 类
      if (hadSmoothScroll) {
        setTimeout(() => {
          if (containerRef.value) {
            containerRef.value.classList.add('scroll-smooth')
            console.log('🟢 [MobileFeed] Restored scroll-smooth')
          }
        }, 100)
      }
    }
  })
}

defineExpose({
  saveScrollPosition,
  restoreScrollPosition
})
</script>

<style scoped>
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
