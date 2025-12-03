<template>
  <div class="flex flex-col h-full">
    <!-- 分类标签 -->
    <div class="flex gap-3 mb-8 overflow-x-auto pb-2 scrollbar-hide mask-fade-right px-4 md:px-8">
      <button v-for="cat in categories" :key="cat"
        class="px-5 py-2 rounded-full glass-card text-sm text-[var(--muted)] hover:bg-[var(--primary)] hover:text-white border border-[var(--border)] hover:border-transparent transition-all whitespace-nowrap click-spring">
        {{ cat }}
      </button>
    </div>

    <!-- 视频网格 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-6 pb-20 px-4 md:px-8">
      <VideoCard v-for="video in videos" :key="video.id" :video="video" @click="$emit('open-video', video)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import VideoCard from './VideoCard.vue'
import type { VideoVO } from '@/api'

defineProps<{
  videos: VideoVO[]
}>()

defineEmits(['open-video'])

const categories = ['全部', '科技数码', '游戏实况', '影视解说', '生活Vlog', '美食制作', '音乐现场', '极客', '汽车']
</script>