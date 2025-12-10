<template>
  <div class="h-full w-full bg-black overflow-y-scroll snap-y snap-mandatory scroll-smooth no-scrollbar" ref="containerRef" @scroll="handleScroll">
    <div v-for="(video, index) in videos" :key="video.id" class="w-full h-full snap-start" :data-index="index">
      <MobileVideoItem :video="video" :isActive="currentIndex === index" @open-comments="$emit('open-comments', video)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import MobileVideoItem from './MobileVideoItem.vue'
import type { VideoVO } from '@/api'

const props = defineProps<{
  videos: VideoVO[]
}>()

defineEmits(['open-comments'])

const containerRef = ref<HTMLElement | null>(null)
const currentIndex = ref(0)
let scrollTimeout: any = null

const handleScroll = () => {
  if (!containerRef.value) return
  
  // Simple debounce not needed for logic but good for performance
  clearTimeout(scrollTimeout)
  scrollTimeout = setTimeout(() => {
    const scrollTop = containerRef.value!.scrollTop
    const clientHeight = containerRef.value!.clientHeight
    const index = Math.round(scrollTop / clientHeight)
    if (index !== currentIndex.value && index >= 0 && index < props.videos.length) {
      currentIndex.value = index
    }
  }, 50)
}
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
